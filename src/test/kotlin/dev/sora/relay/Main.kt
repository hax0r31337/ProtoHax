package dev.sora.relay

import com.google.gson.JsonParser
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.command.impl.CommandDownloadWorld
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.session.listener.RelayListenerAutoCodec
import dev.sora.relay.session.listener.RelayListenerEncryptedSession
import dev.sora.relay.session.listener.RelayListenerMicrosoftLogin
import dev.sora.relay.session.listener.RelayListenerNetworkSettings
import dev.sora.relay.utils.HttpUtils
import dev.sora.relay.utils.logInfo
import dev.sora.relay.utils.logWarn
import io.netty.util.internal.logging.InternalLoggerFactory
import java.io.File
import java.net.InetSocketAddress
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

val tokenFile = File(".ms_refresh_token")

fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())
    val gameSession = craftSession()

    val dst = InetSocketAddress("127.0.0.1", 19136)
	var loginThread: Thread? = null
    val sessionEncryptor = if(tokenFile.exists()) {
		val deviceInfo = RelayListenerMicrosoftLogin.DEVICE_NINTENDO
		RelayListenerMicrosoftLogin(getMSAccessToken(deviceInfo.appId), deviceInfo).also {
			loginThread = thread {
				it.forceFetchChain()
				println("chain ok")
			}
		}
	} else {
		logWarn("no token file found, you won't able to join xbox authenticated servers")
		RelayListenerEncryptedSession()
	}
    val relay = MinecraftRelay(object : MinecraftRelayListener {
        override fun onSessionCreation(session: MinecraftRelaySession): InetSocketAddress {
            session.listeners.add(RelayListenerNetworkSettings(session))
            session.listeners.add(RelayListenerAutoCodec(session))
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
    relay.bind(InetSocketAddress("0.0.0.0", 19132))
    println("bind")
    ModuleResourcePackSpoof.resourcePackProvider = ModuleResourcePackSpoof.FileSystemResourcePackProvider(File("./resource_packs"))
    Thread.sleep(Long.MAX_VALUE)
}

private fun getMSAccessToken(appId: String): String {
    val token = tokenFile.readText(Charsets.UTF_8)
	// if the last part of the token was uuid, it must be authorization code
    val body = JsonParser.parseReader(if (try { UUID.fromString(token.substring(token.indexOf('.')+1)); true } catch (t: Throwable) { false }) {
        HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
            "client_id=$appId&redirect_uri=https://login.live.com/oauth20_desktop.srf&grant_type=authorization_code&code=$token",
            mapOf("Content-Type" to "application/x-www-form-urlencoded"))
    } else {
            HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
                "client_id=$appId&scope=service::user.auth.xboxlive.com::MBI_SSL&grant_type=refresh_token&redirect_uri=https://login.live.com/oauth20_desktop.srf&refresh_token=$token",
                mapOf("Content-Type" to "application/x-www-form-urlencoded"))
    }.inputStream.reader(Charsets.UTF_8)).asJsonObject
    tokenFile.writeText(body.get("refresh_token").asString)
    return body.get("access_token").asString
}

private fun craftSession() : GameSession {
    val session = GameSession()

    val moduleManager = ModuleManager(session)
    moduleManager.init()

    val commandManager = CommandManager(session)
    commandManager.init(moduleManager)
	commandManager.registerCommand(CommandDownloadWorld(session.eventManager, File("./level")))

    val configManager = SingleFileConfigManager(moduleManager)
    configManager.loadConfig("default")

    // save config automatically
    Timer().schedule(20000L, 20000L) {
        configManager.saveConfig("default")
        logInfo("saving config")
    }

    return session
}
