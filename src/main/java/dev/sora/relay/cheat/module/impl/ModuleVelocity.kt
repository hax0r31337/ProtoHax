package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketInbound

class ModuleVelocity : CheatModule("Velocity") {

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if (event.packet is SetEntityMotionPacket) {
            event.cancel()
        }
    }
}