package dev.sora.relay.session

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler

class SessionCloseHandler(private val callback: (String) -> Unit): BedrockPacketHandler {

    override fun onDisconnect(reason: String) {
        callback(reason)
    }
}