package dev.sora.relay

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.PacketCompressionAlgorithm
import com.nukkitx.protocol.bedrock.packet.ClientToServerHandshakePacket
import com.nukkitx.protocol.bedrock.packet.NetworkSettingsPacket
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializerV11
import com.nukkitx.protocol.bedrock.wrapper.compression.NoCompression
import com.nukkitx.protocol.bedrock.wrapper.compression.SnappyCompression
import com.nukkitx.protocol.bedrock.wrapper.compression.ZlibCompression

open class RakNetRelaySessionListener(private val session: RakNetRelaySession) {

    /**
     * @param packet packet that actually received from the server
     * @return can the packet send to the client or not
     */
    open fun onPacketInbound(packet: BedrockPacket): Boolean {
        if (packet is ServerToClientHandshakePacket) return false
        if (packet is NetworkSettingsPacket) {
            val algorithm = when(packet.compressionAlgorithm) {
                PacketCompressionAlgorithm.ZLIB -> ZlibCompression.INSTANCE
                PacketCompressionAlgorithm.SNAPPY -> SnappyCompression.INSTANCE
                else -> NoCompression.INSTANCE
            }
            if (session.clientSerializer is BedrockWrapperSerializerV11) {
                session.clientSerializer.compressionSerializer = algorithm
            }
            if (session.serverSerializer is BedrockWrapperSerializerV11) {
                session.serverSerializer.compressionSerializer = algorithm
            }
        }
        return true
    }

    /**
     * @param packet packet that actually received from the client
     * @return can the packet send to the server or not
     */
    open fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is ClientToServerHandshakePacket) return false
        return true
    }
}