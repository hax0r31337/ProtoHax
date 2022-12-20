package dev.sora.relay.game.entity

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import com.nukkitx.protocol.bedrock.packet.PlayerHotbarPacket
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.GameSession
import java.util.*

class EntityPlayerSP : EntityPlayer(0L, UUID.randomUUID(), "") {

    override var entityId: Long = 0L
    var heldItemSlot = 0
        private set

    fun teleport(x: Double, y: Double, z: Double, netSession: RakNetRelaySession) {
        move(x, y, z)
        netSession.inboundPacket(MovePlayerPacket().apply {
            runtimeEntityId = entityId
            position = Vector3f.from(x, y, z)
            rotation = Vector3f.from(rotationPitch, rotationYaw, 0f)
            mode = MovePlayerPacket.Mode.NORMAL
        })
    }

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
        } else if (packet is PlayerHotbarPacket && packet.containerId == 0) {
            heldItemSlot = packet.selectedHotbarSlot
        } else if (packet is MobEquipmentPacket && packet.runtimeEntityId == entityId) {
            heldItemSlot = packet.hotbarSlot
        }
    }
}