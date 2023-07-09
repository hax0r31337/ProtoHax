package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityLocalPlayer
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityUnknown
import dev.sora.relay.game.event.EventEntitySpawn
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.registry.itemDefinition
import dev.sora.relay.game.utils.constants.EnumFacing
import dev.sora.relay.game.utils.removeNetInfo
import dev.sora.relay.game.utils.toVector3f
import dev.sora.relay.game.utils.toVector3iFloor
import dev.sora.relay.utils.timing.MillisecondTimer
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.LevelEvent
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerHotbarPacket
import kotlin.math.floor
import kotlin.math.pow

class ModuleCrystalAura : CheatModule("CrystalAura", CheatCategory.COMBAT) {

	private var rangeValue by floatValue("Range", 5f, 3f..10f)
	private var suicideValue by boolValue("Suicide", false)
	private var placeValue by boolValue("Place", true)
	private var delayValue by intValue("Delay", 400, 100..1000)
	private var removeParticlesValue by boolValue("RemoveParticles", true)

	private val EXPLOSION_SIZE = 6f

	private val explodeTimer = MillisecondTimer()
	private val placeTimer = MillisecondTimer()

	private val onTickExplode = handle<EventTick>({ explodeTimer.hasTimePassed(delayValue) }) {
		val rangeSq = rangeValue.pow(2)
		val crystal = session.level.entityMap.values
			.filter { it is EntityUnknown && it.identifier == "minecraft:ender_crystal" && it.distanceSq(session.player) < rangeSq }.map {
				var selfDamage = 0f
				var mostDamage = 0f
				session.level.simulateExplosionDamage(it.vec3Position, EXPLOSION_SIZE, listOf(session.player)) { entity, damage ->
					if (entity == session.player) {
						selfDamage = damage
					} else if (entity is EntityPlayer && damage > mostDamage) {
						mostDamage = damage
					}
				}

				if (selfDamage > mostDamage && !suicideValue) {
					mostDamage = -1f
				}

				CrystalDamage(it, mostDamage, selfDamage)
			}.maxByOrNull { it.mostDamage }

		if (crystal != null && crystal.mostDamage != -1f) {
			session.explodeCrystal(crystal.target as Entity)
			explodeTimer.reset()
		}
	}

	private val onTickPlace = handle<EventTick>({ placeValue && placeTimer.hasTimePassed(delayValue) }) {
		val slot = if (session.player.inventory.hand.itemDefinition.identifier == "minecraft:end_crystal") -1
		else session.player.inventory.searchForItemInHotbar { it.itemDefinition.identifier == "minecraft:end_crystal" }
		if (slot == null) {
			return@handle
		}

		val bases = searchPlaceBase(session, floor(rangeValue).toInt())
		// simulate damage to decide best damage point
		val bestPlace = bases.map {
			var selfDamage = 0f
			var mostDamage = 0f
			session.level.simulateExplosionDamage(it.add(0, 2, 0).toVector3f(), EXPLOSION_SIZE, listOf(session.player)) { entity, damage ->
				if (entity == session.player) {
					selfDamage = damage
				} else if (entity is EntityPlayer && damage > mostDamage) {
					mostDamage = damage
				}
			}

			if (selfDamage > mostDamage && !suicideValue) {
				mostDamage = -1f
			}

			CrystalDamage(it, mostDamage, selfDamage)
		}.maxByOrNull { it.mostDamage }
		if (bestPlace != null && bestPlace.mostDamage != -1f) {
			val originalSlot = if (slot != -1) {
				session.sendPacket(PlayerHotbarPacket().apply {
					selectedHotbarSlot = slot
					isSelectHotbarSlot = true
					containerId = ContainerId.INVENTORY
				})
				session.player.inventory.heldItemSlot
			} else -1

			session.player.useItem(InventoryTransactionPacket().apply {
				actionType = 0
				blockPosition = bestPlace.target as Vector3i
				blockFace = EnumFacing.UP.ordinal
				hotbarSlot = session.player.inventory.heldItemSlot
				itemInHand = session.player.inventory.hand.removeNetInfo()
				playerPosition = session.player.vec3Position
				clickPosition = Vector3f.from(Math.random(), Math.random(), Math.random())
				blockDefinition = session.player.inventory.hand.blockDefinition
			}, 1)
			placeTimer.reset()

			if (slot != -1) {
				session.sendPacket(PlayerHotbarPacket().apply {
					selectedHotbarSlot = originalSlot
					isSelectHotbarSlot = true
					containerId = ContainerId.INVENTORY
				})
			}
		}
	}

	private val handleEntitySpawn = handle<EventEntitySpawn> {
		if (!explodeTimer.hasTimePassed(delayValue) || entity !is EntityUnknown || entity.identifier != "minecraft:ender_crystal" || entity.distance(session.player) > rangeValue)
			return@handle

		var selfDamage = 0f
		var mostDamage = 0f
		session.level.simulateExplosionDamage(entity.vec3Position, EXPLOSION_SIZE, listOf(session.player)) { entity1, damage ->
			if (entity1 == session.player) {
				selfDamage = damage
			} else if (entity1 is EntityPlayer && damage > mostDamage) {
				mostDamage = damage
			}
		}

		if (selfDamage <= mostDamage || suicideValue) {
			session.explodeCrystal(entity)
			explodeTimer.reset()
		}
	}

	private val handlePacketInbound = handle<EventPacketInbound> {
		if (removeParticlesValue && packet is LevelEventPacket && packet.type == LevelEvent.PARTICLE_EXPLOSION) {
			cancel()
		}
	}

	private fun searchPlaceBase(session: GameSession, range: Int): List<Vector3i> {
		val center = session.player.vec3Position.toVector3iFloor()
		val bases = mutableListOf<Vector3i>()
		for (x in center.x-range until center.x+range) {
			for (y in center.y-range until center.y+range) {
				for (z in center.z-range until center.z+range) {
					val block = session.level.getBlockAt(x, y, z)
					if ((block.identifier == "minecraft:obsidian" || block.identifier == "minecraft:bedrock")
							&& session.level.getBlockAt(x, y + 1, z).identifier == "minecraft:air") {
						bases.add(Vector3i.from(x, y, z))
					}
				}
			}
		}
		return bases
	}

	private fun GameSession.explodeCrystal(entity: Entity) {
		player.attackEntity(entity, EntityLocalPlayer.SwingMode.SERVERSIDE)
		level.removeEntity(entity)
	}

	private data class CrystalDamage(val target: Any, val mostDamage: Float, val selfDamage: Float)
}
