package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import com.nukkitx.protocol.bedrock.packet.DisconnectPacket
import dev.sora.relay.game.event.impl.EventPacketInbound

class ModuleAntiKick : CheatModule("AntiKick") {

    @Listen
    fun onPacketOutbound(event: EventPacketInbound) {
        if (event.packet is DisconnectPacket) {
            chat("disconnect: ${event.packet.kickMessage}")
            event.cancel()
        }
    }
}