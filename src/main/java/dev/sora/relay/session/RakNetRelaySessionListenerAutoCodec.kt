package dev.sora.relay.session

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.BedrockPacketCodec
import com.nukkitx.protocol.bedrock.compat.BedrockCompat
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.RequestNetworkSettingsPacket
import com.nukkitx.protocol.bedrock.v291.Bedrock_v291
import com.nukkitx.protocol.bedrock.v313.Bedrock_v313
import com.nukkitx.protocol.bedrock.v332.Bedrock_v332
import com.nukkitx.protocol.bedrock.v340.Bedrock_v340
import com.nukkitx.protocol.bedrock.v354.Bedrock_v354
import com.nukkitx.protocol.bedrock.v361.Bedrock_v361
import com.nukkitx.protocol.bedrock.v388.Bedrock_v388
import com.nukkitx.protocol.bedrock.v389.Bedrock_v389
import com.nukkitx.protocol.bedrock.v390.Bedrock_v390
import com.nukkitx.protocol.bedrock.v407.Bedrock_v407
import com.nukkitx.protocol.bedrock.v408.Bedrock_v408
import com.nukkitx.protocol.bedrock.v419.Bedrock_v419
import com.nukkitx.protocol.bedrock.v422.Bedrock_v422
import com.nukkitx.protocol.bedrock.v428.Bedrock_v428
import com.nukkitx.protocol.bedrock.v431.Bedrock_v431
import com.nukkitx.protocol.bedrock.v440.Bedrock_v440
import com.nukkitx.protocol.bedrock.v448.Bedrock_v448
import com.nukkitx.protocol.bedrock.v465.Bedrock_v465
import com.nukkitx.protocol.bedrock.v471.Bedrock_v471
import com.nukkitx.protocol.bedrock.v475.Bedrock_v475
import com.nukkitx.protocol.bedrock.v486.Bedrock_v486
import com.nukkitx.protocol.bedrock.v503.Bedrock_v503
import com.nukkitx.protocol.bedrock.v527.Bedrock_v527
import com.nukkitx.protocol.bedrock.v534.Bedrock_v534
import com.nukkitx.protocol.bedrock.v544.Bedrock_v544
import com.nukkitx.protocol.bedrock.v557.Bedrock_v557
import com.nukkitx.protocol.bedrock.v560.Bedrock_v560
import com.nukkitx.protocol.bedrock.v567.Bedrock_v567
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.utils.logInfo

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
            Bedrock_v291.V291_CODEC, Bedrock_v313.V313_CODEC, Bedrock_v332.V332_CODEC,
            Bedrock_v340.V340_CODEC, Bedrock_v354.V354_CODEC, Bedrock_v361.V361_CODEC,
            Bedrock_v388.V388_CODEC, Bedrock_v389.V389_CODEC, Bedrock_v390.V390_CODEC,
            Bedrock_v407.V407_CODEC, Bedrock_v408.V408_CODEC, Bedrock_v419.V419_CODEC,
            Bedrock_v422.V422_CODEC, Bedrock_v428.V428_CODEC, Bedrock_v431.V431_CODEC,
            Bedrock_v440.V440_CODEC, Bedrock_v448.V448_CODEC, Bedrock_v471.V471_CODEC,
            Bedrock_v475.V475_CODEC, Bedrock_v448.V448_CODEC, Bedrock_v465.V465_CODEC,
            Bedrock_v471.V471_CODEC, Bedrock_v475.V475_CODEC, Bedrock_v486.V486_CODEC,
            Bedrock_v503.V503_CODEC, Bedrock_v527.V527_CODEC, Bedrock_v534.V534_CODEC,
            Bedrock_v544.V544_CODEC, Bedrock_v557.V557_CODEC, Bedrock_v560.V560_CODEC,
            Bedrock_v567.V567_CODEC
        ).associateBy { it.protocolVersion }

        private fun pickProtocolCodec(version: Int): BedrockPacketCodec {
            var codecResult = BedrockCompat.COMPAT_CODEC
            for ((ver, codec) in protocols) {
                if (ver > version) break
                codecResult = codec
            }
            return codecResult
        }
    }
}