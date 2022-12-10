package dev.sora.relay.game.world

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityItem
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityUnknown

class WorldClient(private val session: GameSession) {

    val entityMap = mutableMapOf<Long, Entity>()

    // TODO: chunk

    fun onPacket(packet: BedrockPacket) {
        if (packet is AddEntityPacket) {
            entityMap[packet.runtimeEntityId] = EntityUnknown(packet.runtimeEntityId, packet.identifier).apply {
                move(packet.position)
                rotate(packet.rotation)
            }
        } else if (packet is AddItemEntityPacket) {
            entityMap[packet.runtimeEntityId] = EntityItem(packet.runtimeEntityId).apply {
                move(packet.position)
            }
        } else if (packet is AddPlayerPacket) {
            entityMap[packet.runtimeEntityId] = EntityPlayer(packet.runtimeEntityId).apply {
                move(packet.position)
                rotate(packet.rotation)
            }
        } else if (packet is RemoveEntityPacket) {
            entityMap.remove(packet.uniqueEntityId)
        } else if (packet is TakeItemEntityPacket) {
            entityMap.remove(packet.itemRuntimeEntityId)
        } else {
            entityMap.values.forEach { entity ->
                entity.onPacket(packet)
            }
        }
    }
}