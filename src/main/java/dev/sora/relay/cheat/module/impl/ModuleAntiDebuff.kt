package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketInbound
import com.nukkitx.protocol.bedrock.packet.MobEffectPacket

class ModuleAntiDebuff : CheatModule("AntiDebuff") {

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet
        if (packet is MobEffectPacket) {
            if(packet.effectId == 9/*反胃*/ || packet.effectId == 15/*失明*/ || packet.effectId == 33/*黑暗*/)
            event.cancel()
        }
    }
}