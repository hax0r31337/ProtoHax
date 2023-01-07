package dev.sora.relay

import com.nukkitx.network.raknet.RakNetClient
import com.nukkitx.network.raknet.RakNetServer
import com.nukkitx.network.raknet.RakNetServerListener
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.network.util.EventLoops
import com.nukkitx.protocol.bedrock.BedrockPacketCodec
import com.nukkitx.protocol.bedrock.compat.BedrockCompat
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

class RakNetRelay(listen: InetSocketAddress, val eventLoopGroup: EventLoopGroup = EventLoops.commonGroup(),
                  val packetCodec: BedrockPacketCodec = BedrockCompat.COMPAT_CODEC) {

    val server = RakNetServer(listen, eventLoopGroup).apply {
        listener = RakNetRelayServerListener()
    }
    var listener: RakNetRelayListener? = null

    fun bind() {
        server.bind().join()
    }

    private fun onSession(serverSession: RakNetServerSession) {
        val serverAddress = listener?.onSessionCreation(serverSession) ?: InetSocketAddress("127.0.0.1", 19132)
        val clientSocket = DatagramChannel.open()
        val clientAddress = InetSocketAddress("0.0.0.0", clientSocket.socket().localPort)
        val relayListener = listener?.onPrepareClientConnection(clientSocket) ?: RakNetRelaySessionListener()

        // use custom bootstrap that allows we to use custom DatagramSocket
        val bootstrap = listener?.getBootstrap() ?: Bootstrap().apply {
//            option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
            group(eventLoopGroup)
            channelFactory { NioDatagramChannel(clientSocket) }
        }

        // launch a raknet client
        val client = RakNetClient(clientAddress, bootstrap, null)
        client.protocolVersion = serverSession.protocolVersion
        client.bind().join()

        // connect to server
        val clientSession = client.connect(serverAddress)

        // construct relay session
        val relaySession = RakNetRelaySession(serverSession, clientSession, eventLoopGroup.next(), packetCodec, relayListener)
        listener?.onSession(relaySession)
    }

    internal inner class RakNetRelayServerListener : RakNetServerListener {
        override fun onConnectionRequest(address: InetSocketAddress, realAddress: InetSocketAddress): Boolean {
            return true
        }

        override fun onQuery(address: InetSocketAddress): ByteArray {
            return listener?.onQuery(address) ?: ByteArray(0)
        }

        override fun onSessionCreation(session: RakNetServerSession) {
            onSession(session)
        }

        override fun onUnhandledDatagram(ctx: ChannelHandlerContext, packet: DatagramPacket) {}
    }
}