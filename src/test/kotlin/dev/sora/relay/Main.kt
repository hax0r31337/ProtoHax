package dev.sora.relay

import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.v557.Bedrock_v557
import io.netty.util.internal.logging.InternalLoggerFactory
import java.net.InetSocketAddress


fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())

    val relay = RakNetRelay(InetSocketAddress("0.0.0.0", 19132), packetCodec = Bedrock_v557.V557_CODEC)
    relay.listener = object : RakNetRelayListener {
        override fun onQuery(address: InetSocketAddress) =
            "MCPE;RakNet Relay;557;1.19.20;0;10;${relay.server.guid};Bedrock level;Survival;1;19132;19132;".toByteArray()

        override fun onSessionCreation(serverSession: RakNetServerSession) =
            InetSocketAddress("127.0.0.1", 19136)

        override fun onSession(session: RakNetRelaySession) {
            println("SESSION")
            session.listener = object : RakNetRelaySessionListener(session = session) {
                override fun onPacketInbound(packet: BedrockPacket): Boolean {
//                    if (packet !is LevelChunkPacket) {
//                        println(packet.javaClass)
//                    }
                    return super.onPacketInbound(packet)
                }

                override fun onPacketOutbound(packet: BedrockPacket): Boolean {
//                    println(packet)
                    return super.onPacketOutbound(packet)
                }
            }
        }
    }
    relay.bind()
    println("bind")
    Thread.sleep(Long.MAX_VALUE)
}