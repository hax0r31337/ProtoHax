package dev.sora.relay.session.listener

import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.logInfo
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket

class RelayListenerNetworkSettings(private val session: MinecraftRelaySession) : MinecraftRelayPacketListener {

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        if (packet is NetworkSettingsPacket) {
            logInfo("selected compression algorithm: ${packet.compressionAlgorithm}")
            session.client!!.setCompression(packet.compressionAlgorithm)
            session.sendPacketImmediately(packet)
            session.setCompression(packet.compressionAlgorithm)
            return false
        }

        return true
    }
}
