package dev.sora.relay

import com.google.gson.JsonParser
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.session.listener.RelayListenerAutoCodec
import dev.sora.relay.session.listener.RelayListenerMicrosoftLogin
import dev.sora.relay.session.listener.RelayListenerNetworkSettings
import dev.sora.relay.utils.HttpUtils
import dev.sora.relay.utils.logInfo
import io.netty.util.internal.logging.InternalLoggerFactory
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket
import java.io.File
import java.net.InetSocketAddress
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread


fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())
    val gameSession = craftSession()

    val dst = InetSocketAddress("127.0.0.1", 19136)
    val deviceInfo = RelayListenerMicrosoftLogin.DEVICE_NINTENDO
    val msSession = RelayListenerMicrosoftLogin(getMSAccessToken(deviceInfo.appId), deviceInfo).also {
        thread {
            it.forceFetchChain()
            println("chain ok")
        }
    }
    val relay = MinecraftRelay(object : MinecraftRelayListener {
        override fun onSessionCreation(session: MinecraftRelaySession): InetSocketAddress {
            session.listeners.add(RelayListenerNetworkSettings(session))
            session.listeners.add(RelayListenerAutoCodec(session))
			gameSession.netSession = session
            session.listeners.add(gameSession)
			msSession.session = session
			session.listeners.add(msSession)
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
    val file = File(".ms_refresh_token")
    val token = file.readText(Charsets.UTF_8)
    val body = JsonParser.parseReader(if (token.length == 45) {
        HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
            "client_id=$appId&redirect_uri=https://login.live.com/oauth20_desktop.srf&grant_type=authorization_code&code=$token",
            mapOf("Content-Type" to "application/x-www-form-urlencoded"))
    } else {
            HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
                "client_id=$appId&scope=service::user.auth.xboxlive.com::MBI_SSL&grant_type=refresh_token&redirect_uri=https://login.live.com/oauth20_desktop.srf&refresh_token=$token",
                mapOf("Content-Type" to "application/x-www-form-urlencoded"))
    }.inputStream.reader(Charsets.UTF_8)).asJsonObject
    file.writeText(body.get("refresh_token").asString)
    return body.get("access_token").asString
}

private fun craftSession() : GameSession {
    val session = GameSession()

    val moduleManager = ModuleManager(session)
    moduleManager.init()

    val commandManager = CommandManager(session)
    commandManager.init(moduleManager)

    val configManager = SingleFileConfigManager(moduleManager)
    configManager.loadConfig("default")

    // save config automatically
    Timer().schedule(20000L, 20000L) {
        configManager.saveConfig("default")
        logInfo("saving config")
    }

    return session
}
