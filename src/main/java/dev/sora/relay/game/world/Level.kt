package dev.sora.relay.game.world

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityItem
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityUnknown
import dev.sora.relay.game.event.*
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.*
import java.util.*
import kotlin.math.pow

class Level(session: GameSession, eventManager: EventManager) : ChunkStorage(session, eventManager) {

    val entityMap = mutableMapOf<Long, Entity>()
    val playerList = mutableMapOf<UUID, PlayerListPacket.Entry>()

	private val handleDisconnect = handle<EventDisconnect> {
		entityMap.clear()
		playerList.clear()
	}

	private val handlePacketInbound = handle<EventPacketInbound> {

		if (packet is StartGamePacket) {
			entityMap.clear()
			playerList.clear()
			dimension = packet.dimensionId
		} else if (packet is AddEntityPacket) {
			val entity = EntityUnknown(packet.runtimeEntityId, packet.uniqueEntityId, packet.identifier).apply {
				move(packet.position)
				rotate(packet.rotation)
				handleSetData(packet.metadata)
				handleSetAttribute(packet.attributes)
			}
			entityMap[packet.runtimeEntityId] = entity
			session.eventManager.emit(EventEntitySpawn(session, entity))
		} else if (packet is AddItemEntityPacket) {
			val entity = EntityItem(packet.runtimeEntityId, packet.uniqueEntityId).apply {
				move(packet.position)
				handleSetData(packet.metadata)
			}
			entityMap[packet.runtimeEntityId] = entity
			session.eventManager.emit(EventEntitySpawn(session, entity))
		} else if (packet is AddPlayerPacket) {
			val entity = EntityPlayer(packet.runtimeEntityId, packet.uniqueEntityId, packet.uuid, packet.username).apply {
				move(packet.position.add(0f, EntityPlayer.EYE_HEIGHT, 0f))
				rotate(packet.rotation)
				handleSetData(packet.metadata)
			}
			entityMap[packet.runtimeEntityId] = entity
			session.eventManager.emit(EventEntitySpawn(session, entity))
		} else if (packet is RemoveEntityPacket) {
			val entityToRemove = entityMap.values.find { it.uniqueEntityId == packet.uniqueEntityId } ?: return@handle
			entityMap.remove(entityToRemove.runtimeEntityId)
			session.eventManager.emit(EventEntityDespawn(session, entityToRemove))
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
	}

	fun removeEntity(entity: Entity) {
		entityMap.remove(entity.runtimeEntityId) ?: return // entity do not exist client-side
		session.sendPacket(RemoveEntityPacket().apply {
			uniqueEntityId = entity.uniqueEntityId
		})
		session.eventManager.emit(EventEntityDespawn(session, entity))
	}

	fun simulateExplosionDamage(center: Vector3f, size: Float, extraEntities: List<Entity>, damageCallback: (Entity, Float) -> Unit) {
		val explosionSearchSizeSq = (size * 2).pow(2)

		entityMap.values.filter { it.distanceSq(center) < explosionSearchSizeSq }.forEach {
			val distance = it.distance(center) / size

			if (distance <= 1) {
				val impact = 1 - distance
				val damage = ((impact * impact + impact) / 2) * 8 * size + 1
				damageCallback(it, damage)
			}
		}

		extraEntities.filter { it.distanceSq(center) < explosionSearchSizeSq }.forEach {
			val distance = it.distance(center) / size

			if (distance <= 1) {
				val impact = 1 - distance
				val damage = ((impact * impact + impact) / 2) * 8 * size + 1
				damageCallback(it, damage)
			}
		}
	}
}
