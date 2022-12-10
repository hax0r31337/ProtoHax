package dev.sora.relay

import com.google.gson.JsonParser
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.v560.Bedrock_v560
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.command.impl.CommandToggle
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.HttpUtils
import io.netty.util.internal.logging.InternalLoggerFactory
import java.io.File
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())
    val gameSession = craftSession()

    val relay = RakNetRelay(InetSocketAddress("0.0.0.0", 19132), packetCodec = Bedrock_v560.V560_CODEC)
//    var dst = InetSocketAddress("mco.mineplex.com", 19132)
    var dst = InetSocketAddress("play.lbsg.net", 19132)
    relay.listener = object : RakNetRelayListener {
        override fun onQuery(address: InetSocketAddress) =
            "MCPE;RakNet Relay;557;1.19.20;0;10;${relay.server.guid};Bedrock level;Survival;1;19132;19132;".toByteArray()

        override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
//            return InetSocketAddress("127.0.0.1", 19136)
            return dst
        }

        override fun onSession(session: RakNetRelaySession) {
            session.listener.childListener.add(gameSession)
            gameSession.netSession = session
            session.listener.childListener.add(RakNetRelaySessionListenerMicrosoft(getMSAccessToken(), session))
            session.listener.childListener.add(object : RakNetRelaySessionListener.PacketListener {
                override fun onPacketInbound(packet: BedrockPacket): Boolean {
//                    if(packet !is MovePlayerPacket && packet !is AnimatePacket) {
//                        println(packet.toString().let {
//                            if (it.contains("\n")) it.split("\n")[0] else it
//                        })
//                    }
//                    if (packet is TransferPacket) {
//                        println("Transfer: ${packet.address}:${packet.port}")
//                        dst = InetSocketAddress(packet.address, packet.port)
//                        packet.address = "192.168.2.103"
//                        packet.port = 19132
//                    }
                    return true
                }
//
                override fun onPacketOutbound(packet: BedrockPacket): Boolean {
//                    if (packet !is PlayerAuthInputPacket) {
//                        println(packet)
//                    }
                    return true
                }
            })
        }
    }
    relay.bind()
    println("bind")
    Thread.sleep(Long.MAX_VALUE)
}

private fun getMSAccessToken(): String {
    val file = File(".ms_refresh_token")
    val body = JsonParser.parseReader(
        HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
        "client_id=00000000441cc96b&scope=service::user.auth.xboxlive.com::MBI_SSL&grant_type=refresh_token&redirect_uri=https://login.live.com/oauth20_desktop.srf&refresh_token=${file.readText(Charsets.UTF_8)}",
        mapOf("Content-Type" to "application/x-www-form-urlencoded")).inputStream.reader(Charsets.UTF_8)).asJsonObject
    file.writeText(body.get("refresh_token").asString)
    return body.get("access_token").asString
}

private fun craftSession() : GameSession {
    val session = GameSession()

    val moduleManager = ModuleManager(session)
    moduleManager.init()

    val commandManager = CommandManager(session)
    commandManager.registerCommand(CommandToggle(moduleManager))

    session.eventManager.registerListener(commandManager)

    return session
}