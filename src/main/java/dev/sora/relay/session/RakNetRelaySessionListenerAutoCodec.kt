package dev.sora.relay.session

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.BedrockPacketCodec
import com.nukkitx.protocol.bedrock.compat.BedrockCompat
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.RequestNetworkSettingsPacket
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.utils.logInfo
import java.lang.reflect.Modifier

class RakNetRelaySessionListenerAutoCodec(private val session: RakNetRelaySession) : RakNetRelaySessionListener.PacketListener {

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is RequestNetworkSettingsPacket) {
            session.packetCodec = pickProtocolCodec(packet.protocolVersion)
            logInfo("selected codec (clientProtocol=${packet.protocolVersion}, protocol=${session.packetCodec.protocolVersion}, mc=${session.packetCodec.minecraftVersion})")
        } else if (packet is LoginPacket) {
            session.packetCodec = pickProtocolCodec(packet.protocolVersion)
            logInfo("selected codec (clientProtocol=${packet.protocolVersion}, protocol=${session.packetCodec.protocolVersion}, mc=${session.packetCodec.minecraftVersion})")
        }
        return true
    }

    companion object {
        private val protocols = arrayOf(
            291, 313, 332, 340, 354, 361, 388, 389, 390, 407,
            408, 419, 422, 428, 431, 440, 448, 465, 471, 475, 486, 503, 527, 534, 544, 557, 560
        ).associateWith { getProtocolCodec(it) }

        private fun pickProtocolCodec(version: Int): BedrockPacketCodec {
            var codecResult = BedrockCompat.COMPAT_CODEC
            for ((ver, codec) in protocols) {
                if (ver > version) break
                codecResult = codec
            }
            return codecResult
        }

        private fun getProtocolCodec(version: Int): BedrockPacketCodec {
            val klass = Class.forName("com.nukkitx.protocol.bedrock.v$version.Bedrock_v$version")
            klass.fields.forEach {
                if(Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers)) {
                    val value = it.get(null)
                    if(value is BedrockPacketCodec) {
                        return value
                    }
                }
            }
            throw IllegalStateException("no codec found")
        }
    }
}