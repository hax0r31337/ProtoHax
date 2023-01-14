package dev.sora.relay.game.entity

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import java.util.*

open class EntityPlayer(entityId: Long, open val uuid: UUID, open val username: String) : Entity(entityId) {

    override fun onPacket(packet: BedrockPacket) {
        super.onPacket(packet)
        if (packet is MovePlayerPacket && packet.runtimeEntityId == entityId) {
            move(packet.position.x.toDouble(), packet.position.y.toDouble(), packet.position.z.toDouble())
            rotate(packet.rotation)
            tickExists++
        }
    }
}