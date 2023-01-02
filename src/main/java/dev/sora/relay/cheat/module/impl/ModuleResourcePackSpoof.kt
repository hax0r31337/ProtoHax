package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.ResourcePackStackPacket
import com.nukkitx.protocol.bedrock.packet.ResourcePacksInfoPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketInbound

class ModuleResourcePackSpoof : CheatModule("ResourcePackSpoof") {

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet

        if (packet is ResourcePacksInfoPacket) {
            packet.resourcePackInfos.clear()
            packet.behaviorPackInfos.clear()
        } else if (packet is ResourcePackStackPacket) {
            packet.resourcePacks.clear()
            packet.behaviorPacks.clear()
        }
    }
}