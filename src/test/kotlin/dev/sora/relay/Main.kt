package dev.sora.relay

import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.command.impl.CommandDownloadWorld
import dev.sora.relay.cheat.config.section.ConfigSectionModule
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.misc.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.session.listener.RelayListenerAutoCodec
import dev.sora.relay.session.listener.RelayListenerEncryptedSession
import dev.sora.relay.session.listener.RelayListenerNetworkSettings
import dev.sora.relay.session.listener.xbox.RelayListenerXboxLogin
import dev.sora.relay.session.listener.xbox.XboxDeviceInfo
import dev.sora.relay.session.listener.xbox.cache.XboxIdentityTokenCacheFileSystem
import dev.sora.relay.utils.logInfo
import dev.sora.relay.utils.logWarn
import io.netty.util.internal.logging.InternalLoggerFactory
import java.io.File
import java.net.InetSocketAddress
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

val tokenFile = File(".ms_refresh_token")
val tokenCacheFile = File(".token_cache.json")

fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())
    val gameSession = craftSession()

    val dst = InetSocketAddress("127.0.0.1", 19132)
	var loginThread: Thread? = null
    val sessionEncryptor = if(tokenFile.exists() && !args.contains("--offline")) {
		val deviceInfo = XboxDeviceInfo.DEVICE_NINTENDO
		RelayListenerXboxLogin({
			val (accessToken, refreshToken) = deviceInfo.refreshToken(tokenFile.readText())
			tokenFile.writeText(refreshToken)
			accessToken
		}, deviceInfo).also {
			it.tokenCache = XboxIdentityTokenCacheFileSystem(tokenCacheFile, "account")
			loginThread = thread {
				it.forceFetchChain()
				println("chain ok")
			}
		}
	} else {
		logWarn("Logged in as Offline Mode, you won't able to join xbox authenticated servers")
		RelayListenerEncryptedSession()
	}
    val relay = MinecraftRelay(object : MinecraftRelayListener {
        override fun onSessionCreation(session: MinecraftRelaySession): InetSocketAddress {
            session.listeners.add(RelayListenerNetworkSettings(session))
            session.listeners.add(RelayListenerAutoCodec(session))
//			session.listeners.add(RelayListenerResourcePackDownloader(session, File("./downloaded_resource_packs")))
			gameSession.netSession = session
            session.listeners.add(gameSession)
			loginThread?.also {
				if (it.isAlive) it.join()
				loginThread = null
			}
			sessionEncryptor.session = session
			session.listeners.add(sessionEncryptor)
//            session.listeners.add(object : MinecraftRelayPacketListener {
//                override fun onPacketInbound(packet: BedrockPacket): Boolean {
//                    if (packet is TransferPacket) {
//                        println("Transfer: ${packet.address}:${packet.port}")
//                        dst = InetSocketAddress(packet.address, packet.port)
//                        packet.address = "192.168.2.103"
//                        packet.port = 19132
//                    }
//                    return true
//                }
//
//                override fun onPacketOutbound(packet: BedrockPacket): Boolean {
//                    return true
//                }
//            })

            return dst
        }
    })
    relay.bind(InetSocketAddress("0.0.0.0", 19136))
    println("bind")
    ModuleResourcePackSpoof.resourcePackProvider = ModuleResourcePackSpoof.FileSystemResourcePackProvider(File("./resource_packs"))
    Thread.sleep(Long.MAX_VALUE)
}

private fun craftSession() : GameSession {
    val session = GameSession()

    val moduleManager = ModuleManager(session)
    moduleManager.init()

    val commandManager = CommandManager(session)
    commandManager.init(moduleManager)
	commandManager.registerCommand(CommandDownloadWorld(session.eventManager, File("./level")))

    val configManager = SingleFileConfigManager().apply {
		addSection(ConfigSectionModule(moduleManager))
	}
    configManager.loadConfig("default")

    // save config automatically
    Timer().schedule(20000L, 20000L) {
        configManager.saveConfig("default")
        logInfo("saving config")
    }

    return session
}
