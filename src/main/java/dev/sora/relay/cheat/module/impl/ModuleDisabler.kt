package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketOutbound

class ModuleDisabler : CheatModule("disabler") {

    private val modeValue = ListValue("Mode", arrayOf("LifeBoat"), "LifeBoat")

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet
        if (packet is MovePlayerPacket && modeValue.get() == "LifeBoat") {
            event.session.netSession.outboundPacket(MovePlayerPacket().apply {
                runtimeEntityId = packet.runtimeEntityId
                position = packet.position.add(0f, 0.1f, 0f)
                rotation = packet.rotation
                mode = packet.mode
                isOnGround = packet.isOnGround
            })
        }
    }
}