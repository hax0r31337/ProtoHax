package dev.sora.relay.session

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

interface MinecraftRelayPacketListener {

    fun onPacketInbound(packet: BedrockPacket): Boolean {
        return true
    }

    fun onPacketOutbound(packet: BedrockPacket): Boolean {
        return true
    }

	fun onPacketPostOutbound(packet: BedrockPacket) {
	}

    fun onDisconnect(client: Boolean, reason: String) {

    }
}
