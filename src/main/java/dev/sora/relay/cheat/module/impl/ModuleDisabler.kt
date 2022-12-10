package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketOutbound

class ModuleDisabler : CheatModule("disabler", defaultOn = true) {

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet
        if (packet is MovePlayerPacket) {
            event.session.netSession.outboundPacket(MovePlayerPacket().apply {
                runtimeEntityId = packet.runtimeEntityId
                position = packet.position.add(0f, 0.1f, 0f)
                rotation = packet.rotation
                mode = packet.mode
                isOnGround = packet.isOnGround
            })
        } else if (packet is PlayerAuthInputPacket) {
            event.session.netSession.outboundPacket(PlayerAuthInputPacket().apply {
                rotation = packet.rotation
                position = packet.position.add(0f, 0.1f, 0f)
                motion = packet.motion
                inputData.addAll(packet.inputData)
                inputMode = packet.inputMode
                playMode = packet.playMode
                inputInteractionModel = packet.inputInteractionModel
                vrGazeDirection = packet.vrGazeDirection
                tick = packet.tick
                delta = packet.delta
                itemUseTransaction = packet.itemUseTransaction
                itemStackRequest = packet.itemStackRequest
                playerActions.addAll(packet.playerActions)
            })
        }
    }
}