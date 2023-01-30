package dev.sora.relay

import com.nukkitx.natives.sha256.Sha256
import com.nukkitx.natives.util.Natives
import com.nukkitx.network.raknet.*
import com.nukkitx.network.util.DisconnectReason
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.BedrockPacketCodec
import com.nukkitx.protocol.bedrock.DummyBedrockSession
import com.nukkitx.protocol.bedrock.annotation.Incompressible
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializerV11
import com.nukkitx.protocol.bedrock.wrapper.compression.CompressionSerializer
import com.nukkitx.protocol.bedrock.wrapper.compression.NoCompression
import dev.sora.relay.utils.CipherPair
import dev.sora.relay.utils.logError
import dev.sora.relay.utils.logInfo
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.EventLoop
import io.netty.util.internal.logging.InternalLoggerFactory
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.Deflater


class RakNetRelaySession(val clientsideSession: RakNetServerSession,
                         val serversideSession: RakNetClientSession,
                         private val eventLoop: EventLoop, var packetCodec: BedrockPacketCodec,
                         val listener: RakNetRelaySessionListener) {

    val clientSerializer = listener.provideSerializer(clientsideSession)
    val serverSerializer = listener.provideSerializer(serversideSession)
    private val bedrockSession = DummyBedrockSession(eventLoop)

    var clientCipher: CipherPair? = null
    var serverCipher: CipherPair? = null

    private val pendingPackets = mutableListOf<ByteBuf>()

    private val log = InternalLoggerFactory.getInstance(RakNetRelaySession::class.java)

    init {
        listener.session = this
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

    private fun generateTrailer(buf: ByteBuf, cipherPair: CipherPair): ByteArray? {
        val hash: Sha256 = Natives.SHA_256.get()
        val counterBuf = ByteBufAllocator.DEFAULT.buffer(8)
        return try {
            counterBuf.writeLongLE(cipherPair.sentEncryptedPacketCount.getAndIncrement())
            val keyBuffer = ByteBuffer.wrap(cipherPair.secretKey.encoded)
            hash.update(counterBuf.internalNioBuffer(0, 8))
            hash.update(buf.internalNioBuffer(buf.readerIndex(), buf.readableBytes()))
            hash.update(keyBuffer)
            val digested = hash.digest()
            Arrays.copyOf(digested, 8)
        } finally {
            counterBuf.release()
            hash.reset()
        }
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
            serializer.serialize(compressed, packetCodec, listOf(packet), Deflater.DEFAULT_COMPRESSION, bedrockSession)
            sendSerialized(compressed, isClientside)
        } catch (e: Exception) {
            logError("serialize packet", e)
        } finally {
            compressed?.release()
            if (compression != null) {
                (serializer as BedrockWrapperSerializerV11).compressionSerializer = compression
            }
        }
    }

    private fun sendSerialized(compressed: ByteBuf, isClientside: Boolean) {
        val finalPayload = ByteBufAllocator.DEFAULT.ioBuffer(1 + compressed.readableBytes() + 8)
        finalPayload.writeByte(0xfe) // Wrapped packet ID

        val cipherPair = if (isClientside) clientCipher else serverCipher
        if (cipherPair != null) {
            val trailer = ByteBuffer.wrap(this.generateTrailer(compressed, cipherPair))
            val outBuffer = finalPayload.internalNioBuffer(1, compressed.readableBytes() + 8)
            val inBuffer = compressed.internalNioBuffer(compressed.readerIndex(), compressed.readableBytes())

            cipherPair.encryptionCipher.update(inBuffer, outBuffer)
            cipherPair.encryptionCipher.update(trailer, outBuffer)
            finalPayload.writerIndex(finalPayload.writerIndex() + compressed.readableBytes() + 8)
        } else {
            finalPayload.writeBytes(compressed)
        }
        if (isClientside) {
            clientsideSession.send(finalPayload)
        } else {
            if (serversideSession.state != RakNetState.CONNECTED) {
                pendingPackets.add(finalPayload)
            } else {
                serversideSession.send(finalPayload)
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
                    val packets = try {
                        this.onWrappedPacket(buffer, isClientside)
                    } finally {
                        buffer.release()
                    }
                    packets.forEach {
                        val hold = try {
                            if (isClientside) {
                                listener.onPacketOutbound(it)
                            } else {
                                listener.onPacketInbound(it)
                            }
                        } catch (t: Throwable) {
                            logError("handling packets", t)
                            true
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
        }
    }

    private fun onWrappedPacket(buffer: ByteBuf, isClientside: Boolean): List<BedrockPacket> {
        val cipherPair = if (isClientside) clientCipher else serverCipher
        if (cipherPair != null) {
            val inBuffer: ByteBuffer = buffer.internalNioBuffer(buffer.readerIndex(), buffer.readableBytes())
            val outBuffer = inBuffer.duplicate()
            cipherPair.decryptionCipher.update(inBuffer, outBuffer)

            buffer.writerIndex(buffer.writerIndex() - 8)
        }

        buffer.markReaderIndex()

        if (buffer.isReadable) {
            val packets = mutableListOf<BedrockPacket>()
            (if (isClientside) clientSerializer else serverSerializer).also {
                packetCodec.hasDecodeFailure = false
                it.deserialize(buffer.duplicate(), packetCodec, packets, bedrockSession)
            }
            if (packetCodec.hasDecodeFailure) {
                sendSerialized(buffer, !isClientside)
                log.warn("skipping packets because of failure whilst decode")
                return emptyList()
            }
            return packets
        }
        return emptyList()
    }

    internal inner class RakNetRelayClientListener : RakNetSessionListener {
        override fun onSessionChangeState(state: RakNetState) {
            logInfo("client connection state: $state")
        }

        override fun onDisconnect(reason: DisconnectReason) {
            if (reason == DisconnectReason.DISCONNECTED) return
            serversideSession.disconnect()
            logInfo("client disconnect: $reason")
            listener.onDisconnect(true, reason)
        }

        override fun onEncapsulated(packet: EncapsulatedPacket) {
            readPacketFromBuffer(packet.buffer, true)
        }

        override fun onDirect(buf: ByteBuf) {}
    }

    internal inner class RakNetRelayServerListener : RakNetSessionListener {

        override fun onSessionChangeState(state: RakNetState) {
            logInfo("server connection state: $state")
            // no need for waiting client, cuz client always sends the first packet
            if (state == RakNetState.CONNECTED && pendingPackets.isNotEmpty()) {
                logInfo("pending packets: ${pendingPackets.size}")
                pendingPackets.forEach {
                    serversideSession.send(it)
                }
                pendingPackets.clear()
            }
        }

        override fun onDisconnect(reason: DisconnectReason) {
            if (reason == DisconnectReason.DISCONNECTED) return
            clientsideSession.disconnect()
            logInfo("server disconnect: $reason")
            listener.onDisconnect(false, reason)
        }

        override fun onEncapsulated(packet: EncapsulatedPacket) {
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
    }
}