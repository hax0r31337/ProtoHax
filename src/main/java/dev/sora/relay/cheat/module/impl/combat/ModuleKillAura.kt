package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityLocalPlayer
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.utils.Rotation
import dev.sora.relay.game.utils.constants.Attribute
import dev.sora.relay.game.utils.getRotationDifference
import dev.sora.relay.game.utils.toRotation
import org.cloudburstmc.math.vector.Vector3f
import kotlin.math.pow

class ModuleKillAura : CheatModule("KillAura", CheatCategory.COMBAT) {

    private val cpsValue = clickValue()
    private var rangeValue by floatValue("Range", 3.7f, 2f..7f)
    private var attackModeValue by listValue("AttackMode", AttackMode.values(), AttackMode.SINGLE)
    private var rotationModeValue by listValue("RotationMode", RotationMode.values(), RotationMode.LOCK)
    private var swingValue by listValue("Swing", EntityLocalPlayer.SwingMode.values(), EntityLocalPlayer.SwingMode.BOTH)
	private var priorityModeValue by listValue("PriorityMode", PriorityMode.values(), PriorityMode.DISTANCE)
	private var reversePriorityValue by boolValue("ReversePriority", false)
	private var mouseoverValue by boolValue("Mouseover", false)
    private var swingSoundValue by boolValue("SwingSound", true)
    private var failRateValue by floatValue("FailRate", 0f, 0f..1f)
    private var failSoundValue by boolValue("FailSound", true).visible { failRateValue > 0f }

	private val handleTick = handle<EventTick> {
		val range = rangeValue.pow(2)
		val moduleTargets = moduleManager.getModule(ModuleTargets::class.java)
		val entityList = session.level.entityMap.values.filter {
			it.distanceSq(session.player) < range && with(moduleTargets) { it.isTarget() } }
		if (entityList.isEmpty()) return@handle

		val aimTarget = selectEntity(session, entityList)

		if (cpsValue.range.first >= 20 || cpsValue.canClick) {
			if (Math.random() <= failRateValue) {
				session.player.swing(swingValue, failSoundValue)
			} else {
				when(attackModeValue) {
					AttackMode.MULTI -> {
						entityList.forEach { session.player.attackEntity(it, swingValue, swingSoundValue, mouseoverValue) }
					}
					AttackMode.SINGLE -> {
						session.player.attackEntity(aimTarget, swingValue, swingSoundValue, mouseoverValue)
					}
				}
				cpsValue.click()
			}
		}

		rotationModeValue.rotate(session, session.player.vec3Position, aimTarget.vec3Position)?.let {
			session.player.silentRotation = it
		}
	}

	private fun selectEntity(session: GameSession, entityList: List<Entity>): Entity {
		return when (priorityModeValue) {
			PriorityMode.DISTANCE -> entityList.sortedBy { it.distanceSq(session.player) }
			PriorityMode.HEALTH -> entityList.sortedBy { it.attributes[Attribute.HEALTH]?.value ?: 0f }
			PriorityMode.DIRECTION -> {
				val playerRotation = Rotation(session.player.rotationYaw, session.player.rotationPitch)
				val vec3Position = session.player.vec3Position
				entityList.sortedBy { getRotationDifference(playerRotation, toRotation(vec3Position, it.vec3Position)) }
			}
		}.let { if (!reversePriorityValue) it.first() else it.last() }
	}

	private enum class AttackMode(override val choiceName: String) : NamedChoice {
        SINGLE("Single"),
        MULTI("Multi")
    }

	private enum class RotationMode(override val choiceName: String) : NamedChoice {
		/**
		 * blatant rotation
		 */
        LOCK("Lock") {
			override fun rotate(session: GameSession, source: Vector3f, target: Vector3f): Rotation {
				return toRotation(source, target)
			}
		},
		/**
		 * represents a touch screen liked rotation
		 */
		APPROXIMATE("Approximate") {
			override fun rotate(session: GameSession, source: Vector3f, target: Vector3f): Rotation {
				val aimTarget = toRotation(source, target).let {
					Rotation(it.yaw, it.pitch / 2)
				}
				val last = session.player.lastRotationServerside
				val diff = getRotationDifference(session.player.lastRotationServerside, aimTarget)
				return if (diff < 50) {
					last
				} else {
					Rotation((aimTarget.yaw - last.yaw) / 0.8f + last.yaw, (aimTarget.pitch - last.pitch) / 0.6f + last.pitch)
				}
			}
		},
        NONE("None") {
			override fun rotate(session: GameSession, source: Vector3f, target: Vector3f): Rotation? {
				return null
			}
		};

		abstract fun rotate(session: GameSession, source: Vector3f, target: Vector3f): Rotation?
    }

	private enum class PriorityMode(override val choiceName: String) : NamedChoice {
		DISTANCE("Distance"),
		HEALTH("Health"),
		DIRECTION("Direction")
	}
}
