package dev.sora.relay.session

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.RequestNetworkSettingsPacket
import dev.sora.relay.RakNetRelaySessionListener

class RakNetRelaySessionListenerVersionSpoof(val protocolVersion: Int) : RakNetRelaySessionListener.PacketListener {

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is RequestNetworkSettingsPacket) {
            packet.protocolVersion = protocolVersion
        } else if (packet is LoginPacket) {
            packet.protocolVersion = protocolVersion
        }
        return true
    }
}