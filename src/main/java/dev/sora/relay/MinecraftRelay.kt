package dev.sora.relay

import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelaySession
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption
import org.cloudburstmc.protocol.bedrock.BedrockClientSession
import org.cloudburstmc.protocol.bedrock.BedrockPeer
import org.cloudburstmc.protocol.bedrock.BedrockPong
import org.cloudburstmc.protocol.bedrock.BedrockServerSession
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec
import org.cloudburstmc.protocol.bedrock.codec.compat.BedrockCompat
import org.cloudburstmc.protocol.bedrock.codec.v567.Bedrock_v567
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockClientInitializer
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer
import java.net.InetSocketAddress


class MinecraftRelay(private val listener: MinecraftRelayListener,
                     val motd: BedrockPong = DEFAULT_PONG,
                     val packetCodec: BedrockCodec = BedrockCompat.CODEC) {

    fun bind(address: InetSocketAddress) {
        motd
            .ipv4Port(address.port)
            .ipv6Port(address.port)
        ServerBootstrap()
            .channelFactory(RakChannelFactory.server(NioDatagramChannel::class.java))
            .option(RakChannelOption.RAK_ADVERTISEMENT, motd.toByteBuf())
            .group(NioEventLoopGroup())
            .childHandler(BedrockRelayInitializer())
            .bind(address)
            .syncUninterruptibly()
    }

    inner class BedrockRelayInitializer : BedrockServerInitializer() {

        override fun createSession0(peer: BedrockPeer, subClientId: Int): BedrockServerSession {
            val session = MinecraftRelaySession(peer, subClientId)

            println(peer.socketAddress)

            // establish connection to actual server
            Bootstrap()
                .channelFactory(RakChannelFactory.client(NioDatagramChannel::class.java))
                .group(NioEventLoopGroup())
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, peer.channel.config().getOption(RakChannelOption.RAK_PROTOCOL_VERSION))
                .handler(object : BedrockClientInitializer() {
                    override fun createSession0(peer: BedrockPeer, subClientId: Int): BedrockClientSession {
                        return session.MinecraftRelayClientSession(peer, subClientId).also {
                            session.client = it
                        }
                    }

                    override fun initSession(session: BedrockClientSession) {}
                })
                .bind(listener.onSessionCreation(session))
                .syncUninterruptibly()

            return session
        }

        override fun initSession(session: BedrockServerSession) {
            session.codec = packetCodec
        }
    }

    companion object {
        private val DEFAULT_PONG = BedrockPong()
            .edition("MCPE")
            .motd("ProtoHax")
            .playerCount(0)
            .maximumPlayerCount(20)
            .gameType("Survival")
            .nintendoLimited(false)
            .version(GameSession.RECOMMENDED_VERSION)
            .protocolVersion(Bedrock_v567.CODEC.protocolVersion)
    }
}