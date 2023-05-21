package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.entity.EntityUnknown
import dev.sora.relay.game.event.EventEntitySpawn
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.registry.itemDefinition
import dev.sora.relay.game.utils.constants.EnumFacing
import dev.sora.relay.game.utils.removeNetInfo
import dev.sora.relay.game.utils.toVector3f
import dev.sora.relay.game.utils.toVector3iFloor
import dev.sora.relay.utils.timing.TheTimer
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.LevelEvent
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerHotbarPacket
import kotlin.math.floor
import kotlin.math.pow

class ModuleCrystalAura : CheatModule("CrystalAura") {

	private var rangeValue by floatValue("Range", 5f, 3f..10f)
	private var suicideValue by boolValue("Suicide", false)
	private var placeValue by boolValue("Place", true)
	private var delayValue by intValue("Delay", 400, 100..1000)
	private var removeParticlesValue by boolValue("RemoveParticles", true)

	private val EXPLOSION_SIZE = 6f

	private val explodeTimer = TheTimer()
	private val placeTimer = TheTimer()

	private val onTick = handle<EventTick> { event ->
		val session = event.session

		// search for existing crystals first
		if (explodeTimer.hasTimePassed(delayValue)) {
			val rangeSq = rangeValue.pow(2)
			val crystal = session.theWorld.entityMap.values
				.filter { it is EntityUnknown && it.identifier == "minecraft:ender_crystal" && it.distanceSq(session.thePlayer) < rangeSq }.map {
					var selfDamage = 0f
					var mostDamage = 0f
					session.theWorld.simulateExplosionDamage(it.vec3Position, EXPLOSION_SIZE, listOf(session.thePlayer)) { entity, damage ->
						if (entity == session.thePlayer) {
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

		if (placeValue && placeTimer.hasTimePassed(delayValue)) {
			val slot = if (session.thePlayer.inventory.hand.itemDefinition.identifier == "minecraft:end_crystal") -1
				else session.thePlayer.inventory.searchForItemInHotbar { it.itemDefinition.identifier == "minecraft:end_crystal" }
			if (slot == null) {
				return@handle
			}

			val bases = searchPlaceBase(session, floor(rangeValue).toInt())
			// simulate damage to decide best damage point
			val bestPlace = bases.map {
				var selfDamage = 0f
				var mostDamage = 0f
				session.theWorld.simulateExplosionDamage(it.add(0, 2, 0).toVector3f(), EXPLOSION_SIZE, listOf(session.thePlayer)) { entity, damage ->
					if (entity == session.thePlayer) {
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
					session.thePlayer.inventory.heldItemSlot
				} else -1

				session.thePlayer.useItem(ItemUseTransaction().apply {
					actionType = 0
					blockPosition = bestPlace.target as Vector3i
					blockFace = EnumFacing.UP.ordinal
					hotbarSlot = session.thePlayer.inventory.heldItemSlot
					itemInHand = session.thePlayer.inventory.hand.removeNetInfo()
					playerPosition = session.thePlayer.vec3Position
					clickPosition = Vector3f.from(Math.random(), Math.random(), Math.random())
					blockDefinition = session.thePlayer.inventory.hand.blockDefinition
				})
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
	}

	private val handleEntitySpawn = handle<EventEntitySpawn> { event ->
		val entity = event.entity
		val session = event.session
		if (!explodeTimer.hasTimePassed(delayValue) || entity !is EntityUnknown || entity.identifier != "minecraft:ender_crystal" || entity.distance(session.thePlayer) > rangeValue)
			return@handle

		var selfDamage = 0f
		var mostDamage = 0f
		session.theWorld.simulateExplosionDamage(entity.vec3Position, EXPLOSION_SIZE, listOf(session.thePlayer)) { entity1, damage ->
			if (entity1 == session.thePlayer) {
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

	private val handlePacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if (removeParticlesValue && packet is LevelEventPacket && packet.type == LevelEvent.PARTICLE_EXPLOSION) {
			event.cancel()
		}
	}

	private fun searchPlaceBase(session: GameSession, range: Int): List<Vector3i> {
		val center = session.thePlayer.vec3Position.toVector3iFloor()
		val bases = mutableListOf<Vector3i>()
		for (x in center.x-range until center.x+range) {
			for (y in center.y-range until center.y+range) {
				for (z in center.z-range until center.z+range) {
					val block = session.theWorld.getBlockAt(x, y, z)
					if ((block.identifier == "minecraft:obsidian" || block.identifier == "minecraft:bedrock")
							&& session.theWorld.getBlockAt(x, y + 1, z).identifier == "minecraft:air") {
						bases.add(Vector3i.from(x, y, z))
					}
				}
			}
		}
		return bases
	}

	private fun GameSession.explodeCrystal(entity: Entity) {
		thePlayer.attackEntity(entity, EntityPlayerSP.SwingMode.SERVERSIDE)
		theWorld.removeEntity(entity)
	}

	private data class CrystalDamage(val target: Any, val mostDamage: Float, val selfDamage: Float)
}
