package dev.sora.relay

import com.google.gson.JsonParser
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.TransferPacket
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.RakNetRelaySessionListenerAutoCodec
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.HttpUtils
import dev.sora.relay.utils.logInfo
import io.netty.util.internal.logging.InternalLoggerFactory
import java.io.File
import java.net.InetSocketAddress
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread


fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())
    val gameSession = craftSession()

    val relay = RakNetRelay(InetSocketAddress("0.0.0.0", 19132))
    var dst = InetSocketAddress("mco.mineplex.com", 19132)
    dst = InetSocketAddress("127.0.0.1", 19136)
    val deviceInfo = RakNetRelaySessionListenerMicrosoft.DEVICE_NINTENDO
    val msSession = RakNetRelaySessionListenerMicrosoft(getMSAccessToken(deviceInfo.appId), deviceInfo).also {
        thread {
            it.forceFetchChain()
            println("chain ok")
        }
    }
    relay.listener = object : RakNetRelayListener {
        override fun onQuery(address: InetSocketAddress) =
            "MCPE;RakNet Relay;560;1.19.50;0;10;${relay.server.guid};Bedrock level;Survival;1;19132;19132;".toByteArray()

        override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
            return dst
        }

        override fun onSession(session: RakNetRelaySession) {
            session.listener.childListener.add(gameSession)
            gameSession.netSession = session
            msSession.session = session
            session.listener.childListener.add(msSession)
            session.listener.childListener.add(RakNetRelaySessionListenerAutoCodec(session))
            session.listener.childListener.add(object : RakNetRelaySessionListener.PacketListener {
                override fun onPacketInbound(packet: BedrockPacket): Boolean {
                    if (packet is TransferPacket) {
                        println("Transfer: ${packet.address}:${packet.port}")
                        dst = InetSocketAddress(packet.address, packet.port)
                        packet.address = "192.168.2.103"
                        packet.port = 19132
                    }
                    return true
                }

                override fun onPacketOutbound(packet: BedrockPacket): Boolean {
                    return true
                }
            })
        }
    }
    relay.bind()
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

    session.eventManager.registerListener(commandManager)

    val configManager = SingleFileConfigManager(moduleManager)
    configManager.loadConfig("default")

    Timer().schedule(20000L, 20000L) {
        configManager.saveConfig("default")
        logInfo("saving config")
    }

    return session
}