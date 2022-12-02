package dev.sora.relay

import com.nukkitx.network.raknet.*
import com.nukkitx.network.util.DisconnectReason
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.BedrockPacketCodec
import com.nukkitx.protocol.bedrock.DummyBedrockSession
import com.nukkitx.protocol.bedrock.annotation.Incompressible
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializerV11
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializers
import com.nukkitx.protocol.bedrock.wrapper.compression.CompressionSerializer
import com.nukkitx.protocol.bedrock.wrapper.compression.NoCompression
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.EventLoop
import java.util.zip.Deflater


class RakNetRelaySession(val clientsideSession: RakNetServerSession,
                         val serversideSession: RakNetClientSession,
                         private val eventLoop: EventLoop, private val packetCodec: BedrockPacketCodec) {

    private var clientState = RakNetState.INITIALIZING
    private var serverState = RakNetState.INITIALIZING
    var listener = RakNetRelaySessionListener(this)

    val clientSerializer = BedrockWrapperSerializers.getSerializer(clientsideSession.protocolVersion)
    val serverSerializer = if (clientsideSession.protocolVersion != serversideSession.protocolVersion)
        BedrockWrapperSerializers.getSerializer(serversideSession.protocolVersion)
    else clientSerializer

    init {
        serversideSession.listener = RakNetRelayServerListener()
        clientsideSession.listener = RakNetRelayClientListener()
    }

    fun injectInbound(packet: ByteArray) {
        injectInbound(Unpooled.copiedBuffer(packet))
    }

    fun injectInbound(packet: ByteBuf) {
        clientsideSession.send(packet)
    }

    fun injectOutbound(packet: ByteArray) {
        injectOutbound(Unpooled.copiedBuffer(packet))
    }

    fun injectOutbound(packet: ByteBuf) {
        serversideSession.send(packet)
    }

    fun inboundPacket(packet: BedrockPacket) {
        sendWrapped(packet, true)
    }

    fun outboundPacket(packet: BedrockPacket) {
        sendWrapped(packet, false)
    }

    private fun sendWrapped(packet: BedrockPacket, isClientside: Boolean) {
        val serializer = if (isClientside) clientSerializer else serverSerializer

        val compressed = ByteBufAllocator.DEFAULT.ioBuffer()
        var compression: CompressionSerializer? = null
        if (packet.javaClass.isAnnotationPresent(Incompressible::class.java) && serializer is BedrockWrapperSerializerV11) {
            compression = serializer.compressionSerializer
            serializer.compressionSerializer = NoCompression.INSTANCE
        }
        try {
            serializer.serialize(compressed, packetCodec, listOf(packet), Deflater.DEFAULT_COMPRESSION, DummyBedrockSession.INSTANCE)

            val finalPayload = ByteBufAllocator.DEFAULT.ioBuffer(1 + compressed.readableBytes() + 8)
            finalPayload.writeByte(0xfe) // Wrapped packet ID
            finalPayload.writeBytes(compressed)
            (if (isClientside) clientsideSession else serversideSession).send(finalPayload)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            compressed?.release()
            if (compression != null) {
                (serializer as BedrockWrapperSerializerV11).compressionSerializer = compression
            }
        }
    }

    private fun readPacketFromBuffer(buffer: ByteBuf, isClientside: Boolean) {
        val packetId = buffer.readUnsignedByte().toInt()
        if (packetId == 0xfe && buffer.isReadable) {
            // Wrapper packet
            if (eventLoop.inEventLoop()) {
                this.onWrappedPacket(buffer, isClientside)
            } else {
                buffer.retain() // Handling on different thread
                eventLoop.execute {
                    try {
                        this.onWrappedPacket(buffer, isClientside)
                    } finally {
                        buffer.release()
                    }
                }
            }
        }
    }

    private fun onWrappedPacket(buffer: ByteBuf, isClientside: Boolean) {
        buffer.markReaderIndex()

        if (buffer.isReadable) {
            val packets = mutableListOf<BedrockPacket>()
            (if (isClientside) clientSerializer else serverSerializer).deserialize(buffer, packetCodec, packets, DummyBedrockSession.INSTANCE)
            packets.forEach {
                val hold = if (isClientside) {
                    listener.onPacketOutbound(it)
                } else {
                    listener.onPacketInbound(it)
                }
                if (!hold) return@forEach
                if (isClientside) {
                    outboundPacket(it)
                } else {
                    inboundPacket(it)
                }
            }
        }
    }

    internal inner class RakNetRelayClientListener : RakNetSessionListener {
        override fun onSessionChangeState(state: RakNetState) {
            clientState = state
            while (state == RakNetState.CONNECTED && serverState != RakNetState.CONNECTED) {
                safeSleep(1L)
            }
        }

        override fun onDisconnect(reason: DisconnectReason) {
            if (!serversideSession.isClosed) {
                serversideSession.disconnect(reason)
            }
            clientState = RakNetState.CONNECTED
        }

        override fun onEncapsulated(packet: EncapsulatedPacket) {
            if (clientState != RakNetState.CONNECTED) return
            readPacketFromBuffer(packet.buffer, true)
        }

        override fun onDirect(buf: ByteBuf) {}
    }

    internal inner class RakNetRelayServerListener : RakNetSessionListener {
        override fun onSessionChangeState(state: RakNetState) {
            serverState = state
            while (state == RakNetState.CONNECTED && clientState != RakNetState.CONNECTED) {
                safeSleep(1L)
            }
        }

        override fun onDisconnect(reason: DisconnectReason) {
            if (!clientsideSession.isClosed) {
                clientsideSession.disconnect(reason)
            }
            serverState = RakNetState.CONNECTED
        }

        override fun onEncapsulated(packet: EncapsulatedPacket) {
            if (serverState != RakNetState.CONNECTED) return
            readPacketFromBuffer(packet.buffer, false)
        }

        override fun onDirect(buf: ByteBuf) {}
    }

    companion object {
        private fun readBuf(buf: ByteBuf): ByteArray {
            val bytes = ByteArray(buf.readableBytes())
            val readerIndex = buf.readerIndex()
            buf.getBytes(readerIndex, bytes)
            return bytes
        }

        private fun safeSleep(interval: Long) {
            try {
                Thread.sleep(interval)
            } catch (_: InterruptedException) {
            }
        }
    }
}