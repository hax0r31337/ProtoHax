package dev.sora.relay.session

import dev.sora.relay.utils.logError
import dev.sora.relay.utils.logInfo
import org.cloudburstmc.protocol.bedrock.BedrockClientSession
import org.cloudburstmc.protocol.bedrock.BedrockPeer
import org.cloudburstmc.protocol.bedrock.BedrockServerSession
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

class MinecraftRelaySession(peer: BedrockPeer, subClientId: Int) : BedrockServerSession(peer, subClientId) {

    var client: MinecraftRelayClientSession? = null
        set(value) {
            value?.let {
                it.codec = codec
                queuedPackets.forEach { packet ->
                    it.sendPacket(packet)
                }
                queuedPackets.clear()
            }
            field = value
        }
    private val queuedPackets = mutableListOf<BedrockPacket>()

    val listeners = mutableListOf<MinecraftRelayPacketListener>()

    init {
        packetHandler = SessionCloseHandler {
            logInfo("client disconnect: $it")
            try {
                this@MinecraftRelaySession.close(it)
                listeners.forEach { l ->
                    try {
                        l.onDisconnect(true, it)
                    } catch (t: Throwable) {
                        logError("disconnect handle", t)
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    override fun onPacket(wrapper: BedrockPacketWrapper) {
		val packet = wrapper.packet

        listeners.forEach { l ->
            try {
                if (!l.onPacketOutbound(packet)) {
                    return
                }
            } catch (t: Throwable) {
                logError("packet outbound", t)
            }
        }

        outboundPacket(packet)
    }

    override fun setCodec(codec: BedrockCodec) {
        client?.codec = codec
        super.setCodec(codec)
    }

    fun outboundPacket(packet: BedrockPacket) {
		client?.sendPacket(packet) ?: queuedPackets.add(packet)
    }

    fun inboundPacket(packet: BedrockPacket) {
        sendPacket(packet)
    }

    inner class MinecraftRelayClientSession(peer: BedrockPeer, subClientId: Int) : BedrockClientSession(peer, subClientId) {

        init {
            packetHandler = SessionCloseHandler {
                logInfo("server disconnect: $it")
                try {
                    this@MinecraftRelaySession.close(it)
                    listeners.forEach { l ->
                        try {
                            l.onDisconnect(true, it)
                        } catch (t: Throwable) {
                            logError("disconnect handle", t)
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        override fun onPacket(wrapper: BedrockPacketWrapper) {
			val packet = wrapper.packet

            listeners.forEach { l ->
                try {
                    if (!l.onPacketInbound(packet)) {
                        return
                    }
                } catch (t: Throwable) {
                    logError("packet inbound", t)
                }
            }

            inboundPacket(packet)
        }
    }
}
