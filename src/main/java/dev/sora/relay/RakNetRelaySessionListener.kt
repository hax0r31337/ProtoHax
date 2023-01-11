package dev.sora.relay

import com.nukkitx.network.raknet.RakNetSession
import com.nukkitx.network.util.DisconnectReason
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.PacketCompressionAlgorithm
import com.nukkitx.protocol.bedrock.packet.NetworkSettingsPacket
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializer
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializerV11
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializers
import com.nukkitx.protocol.bedrock.wrapper.compression.NoCompression
import com.nukkitx.protocol.bedrock.wrapper.compression.SnappyCompression
import com.nukkitx.protocol.bedrock.wrapper.compression.ZlibCompression
import dev.sora.relay.utils.logError

open class RakNetRelaySessionListener {

    lateinit var session: RakNetRelaySession

    val childListener = mutableListOf<PacketListener>()

    /**
     * @param packet packet that actually received from the server
     * @return can the packet send to the client or not
     */
    open fun onPacketInbound(packet: BedrockPacket): Boolean {
        if (packet is NetworkSettingsPacket) {
            val algorithm = when(packet.compressionAlgorithm) {
                PacketCompressionAlgorithm.ZLIB -> ZlibCompression.INSTANCE
                PacketCompressionAlgorithm.SNAPPY -> SnappyCompression.INSTANCE
                else -> NoCompression.INSTANCE
            }
            if (session.clientSerializer is BedrockWrapperSerializerV11) {
                (session.clientSerializer as BedrockWrapperSerializerV11).compressionSerializer = algorithm
            }
            if (session.serverSerializer is BedrockWrapperSerializerV11) {
                (session.serverSerializer as BedrockWrapperSerializerV11).compressionSerializer = algorithm
            }
        }

        childListener.forEach {
            try {
                if (!it.onPacketInbound(packet)) {
                    return false
                }
            } catch (t: Throwable) {
                logError("packet inbound", t)
            }
        }

        return true
    }

    /**
     * @param packet packet that actually received from the client
     * @return can the packet send to the server or not
     */
    open fun onPacketOutbound(packet: BedrockPacket): Boolean {
        childListener.forEach {
            try {
                if (!it.onPacketOutbound(packet)) {
                    return false
                }
            } catch (t: Throwable) {
                logError("packet outbound", t)
            }
        }
        return true
    }

    open fun onDisconnect(client: Boolean, reason: DisconnectReason) {
        childListener.forEach {
            try {
                it.onDisconnect(client, reason)
            } catch (t: Throwable) {
                logError("disconnect handle", t)
            }
        }
    }

    open fun provideSerializer(session: RakNetSession): BedrockWrapperSerializer {
        return BedrockWrapperSerializers.getSerializer(session.protocolVersion)
    }

    interface PacketListener {

        fun onPacketInbound(packet: BedrockPacket): Boolean {
            return true
        }

        fun onPacketOutbound(packet: BedrockPacket): Boolean {
            return true
        }

        fun onDisconnect(client: Boolean, reason: DisconnectReason) {

        }
    }
}