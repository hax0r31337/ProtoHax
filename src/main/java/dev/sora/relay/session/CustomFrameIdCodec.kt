package dev.sora.relay.session

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.util.internal.StringUtil
import org.cloudburstmc.netty.channel.raknet.RakReliability
import org.cloudburstmc.netty.channel.raknet.packet.RakMessage
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockChannelInitializer

/**
 * custom [FrameIdCodec] with reliability support
 */
@Sharable
class CustomFrameIdCodec(private val frameId: Int = BedrockChannelInitializer.RAKNET_MINECRAFT_ID,
						 private val reliability: RakReliability? = null) : MessageToMessageCodec<Any, ByteBuf>() {

    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        val buf = ctx.alloc().compositeDirectBuffer(2)
        try {
            buf.addComponent(true, ctx.alloc().ioBuffer(1).writeByte(frameId))
            buf.addComponent(true, msg.retainedSlice())
			if (reliability == null) {
				out.add(buf.retain())
			} else {
				out.add(RakMessage(buf.retain(), reliability))
			}
        } finally {
            buf.release()
        }
    }

    override fun decode(ctx: ChannelHandlerContext, msg: Any, out: MutableList<Any>) {
		val content = if (msg is RakMessage) {
			msg.content()
		} else if (msg is ByteBuf) {
			msg
		} else {
			throw UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg))
		}

		if (!content.isReadable) {
			return
		}
		val id = content.readUnsignedByte().toInt()
		check(id == frameId) { "Invalid frame ID: $id" }
		out.add(content.readRetainedSlice(content.readableBytes()))
    }

    companion object {
        const val NAME = "frame-id-codec-custom"
		val INSTANCE = CustomFrameIdCodec()
    }
}
