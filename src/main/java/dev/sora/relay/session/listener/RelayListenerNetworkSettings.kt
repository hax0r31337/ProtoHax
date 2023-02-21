package dev.sora.relay.session.listener

import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket

class RelayListenerNetworkSettings(private val session: MinecraftRelaySession) : MinecraftRelayPacketListener {

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        if (packet is NetworkSettingsPacket) {
            session.client!!.setCompression(packet.compressionAlgorithm)
            // TODO: @Incompressible annotation takes no effect on nukkit protocol library
            session.sendPacketImmediately(packet)
            session.setCompression(packet.compressionAlgorithm)
            return false
        }

        return true
    }
}