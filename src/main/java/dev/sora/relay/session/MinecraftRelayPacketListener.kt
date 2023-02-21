package dev.sora.relay.session

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

interface MinecraftRelayPacketListener {

    fun onPacketInbound(packet: BedrockPacket): Boolean {
        return true
    }

    fun onPacketOutbound(packet: BedrockPacket): Boolean {
        return true
    }

    fun onDisconnect(client: Boolean, reason: String) {

    }
}