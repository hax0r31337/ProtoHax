package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketOutbound
import com.nukkitx.protocol.bedrock.packet.DisconnectPacket

class ModuleAntiKick : CheatModule("AntiKick") {

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        if (event.packet is DisconnectPacket) {
            event.cancel()
        }
    }
}