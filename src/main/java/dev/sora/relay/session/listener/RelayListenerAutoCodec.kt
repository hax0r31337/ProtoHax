package dev.sora.relay.session.listener

import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.logInfo
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec
import org.cloudburstmc.protocol.bedrock.codec.compat.BedrockCompat
import org.cloudburstmc.protocol.bedrock.codec.v291.Bedrock_v291
import org.cloudburstmc.protocol.bedrock.codec.v313.Bedrock_v313
import org.cloudburstmc.protocol.bedrock.codec.v332.Bedrock_v332
import org.cloudburstmc.protocol.bedrock.codec.v340.Bedrock_v340
import org.cloudburstmc.protocol.bedrock.codec.v354.Bedrock_v354
import org.cloudburstmc.protocol.bedrock.codec.v361.Bedrock_v361
import org.cloudburstmc.protocol.bedrock.codec.v388.Bedrock_v388
import org.cloudburstmc.protocol.bedrock.codec.v389.Bedrock_v389
import org.cloudburstmc.protocol.bedrock.codec.v390.Bedrock_v390
import org.cloudburstmc.protocol.bedrock.codec.v407.Bedrock_v407
import org.cloudburstmc.protocol.bedrock.codec.v408.Bedrock_v408
import org.cloudburstmc.protocol.bedrock.codec.v419.Bedrock_v419
import org.cloudburstmc.protocol.bedrock.codec.v422.Bedrock_v422
import org.cloudburstmc.protocol.bedrock.codec.v428.Bedrock_v428
import org.cloudburstmc.protocol.bedrock.codec.v431.Bedrock_v431
import org.cloudburstmc.protocol.bedrock.codec.v440.Bedrock_v440
import org.cloudburstmc.protocol.bedrock.codec.v448.Bedrock_v448
import org.cloudburstmc.protocol.bedrock.codec.v465.Bedrock_v465
import org.cloudburstmc.protocol.bedrock.codec.v471.Bedrock_v471
import org.cloudburstmc.protocol.bedrock.codec.v475.Bedrock_v475
import org.cloudburstmc.protocol.bedrock.codec.v486.Bedrock_v486
import org.cloudburstmc.protocol.bedrock.codec.v503.Bedrock_v503
import org.cloudburstmc.protocol.bedrock.codec.v527.Bedrock_v527
import org.cloudburstmc.protocol.bedrock.codec.v534.Bedrock_v534
import org.cloudburstmc.protocol.bedrock.codec.v544.Bedrock_v544
import org.cloudburstmc.protocol.bedrock.codec.v557.Bedrock_v557
import org.cloudburstmc.protocol.bedrock.codec.v560.Bedrock_v560
import org.cloudburstmc.protocol.bedrock.codec.v567.Bedrock_v567
import org.cloudburstmc.protocol.bedrock.codec.v575.Bedrock_v575
import org.cloudburstmc.protocol.bedrock.codec.v582.Bedrock_v582
import org.cloudburstmc.protocol.bedrock.codec.v589.Bedrock_v589
import org.cloudburstmc.protocol.bedrock.codec.v594.Bedrock_v594
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket

class RelayListenerAutoCodec(private val session: MinecraftRelaySession) : MinecraftRelayPacketListener {

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is RequestNetworkSettingsPacket) {
            val codec = pickProtocolCodec(packet.protocolVersion)
            session.codec = codec
            logInfo("selected codec (clientProtocol=${packet.protocolVersion}, protocol=${codec.protocolVersion}, mc=${codec.minecraftVersion})")
        } else if (packet is LoginPacket) {
            val codec = pickProtocolCodec(packet.protocolVersion)
            session.codec = codec
            logInfo("selected codec (clientProtocol=${packet.protocolVersion}, protocol=${codec.protocolVersion}, mc=${codec.minecraftVersion})")
        }
        return true
    }

    companion object {
        private val protocols = arrayOf(
            Bedrock_v291.CODEC, Bedrock_v313.CODEC, Bedrock_v332.CODEC,
            Bedrock_v340.CODEC, Bedrock_v354.CODEC, Bedrock_v361.CODEC,
            Bedrock_v388.CODEC, Bedrock_v389.CODEC, Bedrock_v390.CODEC,
            Bedrock_v407.CODEC, Bedrock_v408.CODEC, Bedrock_v419.CODEC,
            Bedrock_v422.CODEC, Bedrock_v428.CODEC, Bedrock_v431.CODEC,
            Bedrock_v440.CODEC, Bedrock_v448.CODEC, Bedrock_v471.CODEC,
            Bedrock_v475.CODEC, Bedrock_v448.CODEC, Bedrock_v465.CODEC,
            Bedrock_v471.CODEC, Bedrock_v475.CODEC, Bedrock_v486.CODEC,
            Bedrock_v503.CODEC, Bedrock_v527.CODEC, Bedrock_v534.CODEC,
            Bedrock_v544.CODEC, Bedrock_v557.CODEC, Bedrock_v560.CODEC,
            Bedrock_v567.CODEC, Bedrock_v575.CODEC, Bedrock_v582.CODEC,
			Bedrock_v589.CODEC, Bedrock_v594.CODEC
        ).associateBy { it.protocolVersion }

        private fun pickProtocolCodec(version: Int): BedrockCodec {
            var codecResult = BedrockCompat.CODEC
            for ((ver, codec) in protocols) {
                if (ver > version) break
                codecResult = codec
            }
            return codecResult
        }
    }
}
