package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import com.nukkitx.protocol.bedrock.packet.MobEffectPacket

class ModuleNightVision : CheatModule("NightVision") {
    override fun onEnable() {
        session.netSession.inboundPacket(MobEffectPacket().apply {
            event = MobEffectPacket.Event.ADD
            effectId = 16 //夜视
            amplifier = 0
            isParticles = false
            duration = 100000
        })
    }

    override fun onDisable() {
        session.netSession.inboundPacket(MobEffectPacket().apply {
            event = MobEffectPacket.Event.REMOVE
            effectId = 16
        })
    }
}