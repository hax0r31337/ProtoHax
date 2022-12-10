package dev.sora.relay.game.entity

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import com.nukkitx.protocol.bedrock.packet.PlayerHotbarPacket
import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.GameSession

class EntityPlayerSP : EntityPlayer(0L) {

    override var entityId: Long = 0L
    var heldItemSlot = 0
        private set

    fun handleClientPacket(packet: BedrockPacket, session: GameSession) {
        if (packet is MovePlayerPacket) {
            move(packet.position)
            rotate(packet.rotation)
            if (packet.runtimeEntityId != entityId) {
                BasicThing.chat(session, "runtimeEntityId mismatch, desync occur? (client=${packet.runtimeEntityId}, relay=${entityId})")
                entityId = packet.runtimeEntityId
            }
            session.onTick()
        } else if (packet is PlayerAuthInputPacket) {
            move(packet.position)
            rotate(packet.rotation)
            session.onTick()
        } else if (packet is PlayerHotbarPacket && packet.selectedHotbarSlot == 0) {
            heldItemSlot = packet.selectedHotbarSlot
        }
    }
}