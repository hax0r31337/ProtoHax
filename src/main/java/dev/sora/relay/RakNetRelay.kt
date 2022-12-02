package dev.sora.relay

import com.nukkitx.network.raknet.RakNetClient
import com.nukkitx.network.raknet.RakNetServer
import com.nukkitx.network.raknet.RakNetServerListener
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.network.util.EventLoops
import com.nukkitx.protocol.bedrock.BedrockPacketCodec
import com.nukkitx.protocol.bedrock.compat.BedrockCompat
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.*

class RakNetRelay(listen: InetSocketAddress, private val eventLoopGroup: EventLoopGroup = EventLoops.commonGroup(),
                  private val packetCodec: BedrockPacketCodec = BedrockCompat.COMPAT_CODEC) {

    val server = RakNetServer(listen, eventLoopGroup).apply {
        listener = RakNetRelayServerListener()
    }
    var listener: RakNetRelayListener? = null

    fun bind() {
        server.bind().join()
    }

    private fun onSession(serverSession: RakNetServerSession) {
        val port = try {
            val socket = DatagramSocket()
            val port = socket.localPort
            socket.close()
            port
        } catch (e: Exception) {
            Random().nextInt(65535)
        }

        // launch a raknet client
        val client = RakNetClient(InetSocketAddress("0.0.0.0", port), eventLoopGroup)
        client.protocolVersion = serverSession.protocolVersion
        client.bind().join()

        // connect to server
        val serverAddress = listener?.onSessionCreation(serverSession) ?: InetSocketAddress("127.0.0.1", 19132)
        val clientSession = client.connect(serverAddress)

        // construct relay session
        val relaySession = RakNetRelaySession(serverSession, clientSession, eventLoopGroup.next(), packetCodec)
        listener?.onSession(relaySession)
    }

    internal inner class RakNetRelayServerListener : RakNetServerListener {
        override fun onConnectionRequest(address: InetSocketAddress, realAddress: InetSocketAddress): Boolean {
            return true
        }

        override fun onQuery(address: InetSocketAddress): ByteArray? {
            return listener?.onQuery(address) ?: ByteArray(0)
        }

        override fun onSessionCreation(session: RakNetServerSession) {
            onSession(session)
        }

        override fun onUnhandledDatagram(ctx: ChannelHandlerContext, packet: DatagramPacket) {}
    }
}