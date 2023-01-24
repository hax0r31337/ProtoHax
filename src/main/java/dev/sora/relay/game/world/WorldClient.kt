package dev.sora.relay.game.world

import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityItem
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityUnknown
import dev.sora.relay.game.event.EventDisconnect
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.Listen
import java.util.*

class WorldClient(session: GameSession) : WorldwideBlockStorage(session) {

    val entityMap = mutableMapOf<Long, Entity>()
    val playerList = mutableMapOf<UUID, PlayerListPacket.Entry>()

    @Listen
    override fun onDisconnect(event: EventDisconnect) {
        entityMap.clear()
        playerList.clear()
        super.onDisconnect(event)
    }

    @Listen
    override fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet

        if (packet is StartGamePacket) {
            entityMap.clear()
            playerList.clear()
            dimension = packet.dimensionId
        } else if (packet is AddEntityPacket) {
            entityMap[packet.runtimeEntityId] = EntityUnknown(packet.runtimeEntityId, packet.identifier).apply {
                move(packet.position)
                rotate(packet.rotation)
                handleSetData(packet.metadata)
                handleSetAttribute(packet.attributes)
            }
        } else if (packet is AddItemEntityPacket) {
            entityMap[packet.runtimeEntityId] = EntityItem(packet.runtimeEntityId).apply {
                move(packet.position)
                handleSetData(packet.metadata)
            }
        } else if (packet is AddPlayerPacket) {
            entityMap[packet.runtimeEntityId] = EntityPlayer(packet.runtimeEntityId, packet.uuid, packet.username).apply {
                move(packet.position.add(0f, 1.62f, 0f))
                rotate(packet.rotation)
                handleSetData(packet.metadata)
            }
        } else if (packet is RemoveEntityPacket) {
            entityMap.remove(packet.uniqueEntityId)
        } else if (packet is TakeItemEntityPacket) {
            entityMap.remove(packet.itemRuntimeEntityId)
        } else if (packet is PlayerListPacket) {
            val add = packet.action == PlayerListPacket.Action.ADD
            packet.entries.forEach {
                if (add) {
                    playerList[it.uuid] = it
                } else {
                    playerList.remove(it.uuid)
                }
            }
        } else if (packet is ChangeDimensionPacket) {
            dimension = packet.dimension
        } else {
            entityMap.values.forEach { entity ->
                entity.onPacket(packet)
            }
        }
        super.onPacketInbound(event)
    }
}