package dev.sora.relay.game.entity

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import java.util.*

open class EntityPlayer(entityId: Long, open val uuid: UUID, open val username: String) : Entity(entityId) {

    override fun onPacket(packet: BedrockPacket) {
        super.onPacket(packet)
        if (packet is MovePlayerPacket && packet.runtimeEntityId == entityId) {
            move(packet.position.x, packet.position.y, packet.position.z)
            rotate(packet.rotation)
            tickExists++
        }
    }

	companion object {
		val EYE_HEIGHT = 1.62f
	}
}
