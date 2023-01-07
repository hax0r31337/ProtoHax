package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import com.nukkitx.protocol.bedrock.packet.MobEffectPacket
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.game.event.impl.EventTick

class ModuleFastBreak : CheatModule("FastBreak") {
    private val amplifierValue = IntValue("levels", 5, 1, 128)

    @Listen
    fun onTick(event: EventTick){
        event.session.netSession.inboundPacket(MobEffectPacket().apply {
            setEvent(MobEffectPacket.Event.ADD)
            effectId = 3 //急迫
            amplifier = amplifierValue.get() - 1
            isParticles = false
            duration = 60
        })
    }

    override fun onDisable() {
        session.netSession.inboundPacket(MobEffectPacket().apply {
            event = MobEffectPacket.Event.REMOVE
            effectId = 3 //急迫
        })
    }
}