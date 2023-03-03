package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.utils.constants.Effect
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket

class ModuleAntiBlind : CheatModule("AntiBlind") {

    private var nightVisionValue by boolValue("NightVision", true)
    private var removeBadEffectsValue by boolValue("RemoveBadEffects", true)

    @Listen
    fun onTick(event: EventTick){
        if (!nightVisionValue || event.session.thePlayer.tickExists % 20 != 0L) return
        session.netSession.inboundPacket(MobEffectPacket().apply {
            runtimeEntityId = session.thePlayer.entityId
            setEvent(MobEffectPacket.Event.ADD)
            effectId = Effect.NIGHT_VISION
            amplifier = 0
            isParticles = false
            duration = 360000
        })
    }

    override fun onDisable() {
        if (nightVisionValue && session.netSessionInitialized) {
            session.netSession.inboundPacket(MobEffectPacket().apply {
                event = MobEffectPacket.Event.REMOVE
                runtimeEntityId = session.thePlayer.entityId
                effectId = Effect.NIGHT_VISION
            })
        }
    }

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet
        if (removeBadEffectsValue && packet is MobEffectPacket) {
            if (packet.effectId == Effect.NAUSEA || packet.effectId == Effect.BLINDNESS || packet.effectId == Effect.DARKNESS) {
                event.cancel()
            }
        }
    }
}