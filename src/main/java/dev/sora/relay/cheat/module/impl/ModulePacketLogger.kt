package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.game.event.impl.EventPacketInbound

class ModulePacketLogger : CheatModule("PacketLogger") {

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet
        chat("ClientPacket${packet.packetId} $packet ")
    }

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet
        chat("ServerPacket${packet.packetId} $packet ")
    }
}