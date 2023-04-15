package dev.sora.relay

import dev.sora.relay.game.GameSession
import dev.sora.relay.session.CustomFrameIdCodec
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.logInfo
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory
import org.cloudburstmc.netty.channel.raknet.RakReliability
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption
import org.cloudburstmc.protocol.bedrock.BedrockClientSession
import org.cloudburstmc.protocol.bedrock.BedrockPeer
import org.cloudburstmc.protocol.bedrock.BedrockPong
import org.cloudburstmc.protocol.bedrock.BedrockServerSession
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec
import org.cloudburstmc.protocol.bedrock.codec.compat.BedrockCompat
import org.cloudburstmc.protocol.bedrock.codec.v567.Bedrock_v567
import org.cloudburstmc.protocol.bedrock.netty.codec.FrameIdCodec
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockChannelInitializer
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockClientInitializer
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockServerInitializer
import java.net.InetSocketAddress


open class MinecraftRelay(private val listener: MinecraftRelayListener,
                     val motd: BedrockPong = DEFAULT_PONG,
                     val packetCodec: BedrockCodec = BedrockCompat.CODEC) {

	private var channelFuture: ChannelFuture? = null

	val isRunning: Boolean
		get() = channelFuture != null

	open fun channelFactory(): ChannelFactory<out ServerChannel> {
		return RakChannelFactory.server(NioDatagramChannel::class.java)
	}

    fun bind(address: InetSocketAddress) {
		assert(!isRunning) { "server is already running" }

        motd
            .ipv4Port(address.port)
            .ipv6Port(address.port)
        channelFuture = ServerBootstrap()
            .channelFactory(channelFactory())
            .option(RakChannelOption.RAK_ADVERTISEMENT, motd.toByteBuf())
            .group(NioEventLoopGroup())
            .childHandler(BedrockRelayInitializer())
            .bind(address)
            .syncUninterruptibly()
    }

	fun stop() {
		assert(isRunning) { "server is not running" }

		channelFuture?.channel()?.also {
			it.close().syncUninterruptibly()
			it.parent().close().syncUninterruptibly()
		}
		channelFuture = null
	}

    inner class BedrockRelayInitializer : BedrockServerInitializer() {

        override fun createSession0(peer: BedrockPeer, subClientId: Int): BedrockServerSession {
            val session = MinecraftRelaySession(peer, subClientId)
			logInfo("client connected")

            // establish connection to actual server
            Bootstrap()
                .channelFactory(RakChannelFactory.client(NioDatagramChannel::class.java))
                .group(NioEventLoopGroup())
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, peer.channel.config().getOption(RakChannelOption.RAK_PROTOCOL_VERSION))
                .handler(object : BedrockClientInitializer() {
                    override fun createSession0(peer: BedrockPeer, subClientId: Int): BedrockClientSession {
						logInfo("server connected")
                        return session.MinecraftRelayClientSession(peer, subClientId).also {
                            session.client = it
                        }
                    }

					override fun postInitChannel(channel: Channel) {
						super.postInitChannel(channel)
						// use custom reliability settings
						channel.pipeline().addBefore(FrameIdCodec.NAME, CustomFrameIdCodec.NAME, CustomFrameIdCodec(BedrockChannelInitializer.RAKNET_MINECRAFT_ID, RakReliability.RELIABLE))
						channel.pipeline().remove(FrameIdCodec.NAME)
					}

                    override fun initSession(session: BedrockClientSession) {}
                })
                .connect(listener.onSessionCreation(session))
                .syncUninterruptibly()

            return session
        }

        override fun initSession(session: BedrockServerSession) {
            session.codec = packetCodec
        }

		override fun postInitChannel(channel: Channel) {
			super.postInitChannel(channel)
			// use custom reliability settings
			channel.pipeline().addBefore(FrameIdCodec.NAME, CustomFrameIdCodec.NAME, CustomFrameIdCodec(BedrockChannelInitializer.RAKNET_MINECRAFT_ID))
			channel.pipeline().remove(FrameIdCodec.NAME)
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
