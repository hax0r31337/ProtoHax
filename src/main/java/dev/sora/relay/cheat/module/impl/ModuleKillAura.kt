package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.utils.Rotation
import dev.sora.relay.game.utils.constants.Attribute
import dev.sora.relay.game.utils.getRotationDifference
import dev.sora.relay.game.utils.toRotation
import dev.sora.relay.utils.timing.ClickTimer
import kotlin.math.pow

class ModuleKillAura : CheatModule("KillAura") {

    private var cpsValue by intValue("CPS", 7, 1..20)
    private var rangeValue by floatValue("Range", 3.7f, 2f..7f)
    private var attackModeValue by listValue("AttackMode", AttackMode.values(), AttackMode.SINGLE)
    private var rotationModeValue by listValue("RotationMode", RotationMode.values(), RotationMode.LOCK)
    private var swingValue by listValue("Swing", EntityPlayerSP.SwingMode.values(), EntityPlayerSP.SwingMode.BOTH)
	private var priorityModeValue by listValue("PriorityMode", PriorityMode.values(), PriorityMode.DISTANCE)
	private var reversePriorityValue by boolValue("ReversePriority", false)
    private var swingSoundValue by boolValue("SwingSound", true)
    private var failRateValue by floatValue("FailRate", 0f, 0f..1f)
    private var failSoundValue by boolValue("FailSound", true)

    private val clickTimer = ClickTimer()

	private val handleTick = handle<EventTick> { event ->
		val session = event.session

		val range = rangeValue.pow(2)
		val moduleTargets = moduleManager.getModule(ModuleTargets::class.java)
		val entityList = session.theWorld.entityMap.values.filter {
			it.distanceSq(session.thePlayer) < range && with(moduleTargets) { it.isTarget() } }
		if (entityList.isEmpty()) return@handle

		val aimTarget = if (Math.random() <= failRateValue || (cpsValue < 20 && !clickTimer.canClick())) {
			session.thePlayer.swing(swingValue, failSoundValue)
			selectEntity(session, entityList)
		} else {
			when(attackModeValue) {
				AttackMode.MULTI -> {
					entityList.forEach { session.thePlayer.attackEntity(it, swingValue, swingSoundValue) }
					selectEntity(session, entityList)
				}
				AttackMode.SINGLE -> {
					selectEntity(session, entityList).also {
						session.thePlayer.attackEntity(it, swingValue, swingSoundValue)
					}
				}
			}
		}

		when (rotationModeValue) {
			RotationMode.LOCK -> {
				session.thePlayer.silentRotation = toRotation(session.thePlayer.vec3Position, aimTarget.vec3Position)
			}
			RotationMode.NONE -> {}
		}

		clickTimer.update(cpsValue, cpsValue + 1)
	}

	private fun selectEntity(session: GameSession, entityList: List<Entity>): Entity {
		return when (priorityModeValue) {
			PriorityMode.DISTANCE -> entityList.sortedBy { it.distanceSq(session.thePlayer) }
			PriorityMode.HEALTH -> entityList.sortedBy { it.attributes[Attribute.HEALTH]?.value ?: 0f }
			PriorityMode.DIRECTION -> {
				val playerRotation = Rotation(session.thePlayer.rotationYaw, session.thePlayer.rotationPitch)
				val vec3Position = session.thePlayer.vec3Position
				entityList.sortedBy { getRotationDifference(playerRotation, toRotation(vec3Position, it.vec3Position)) }
			}
		}.let { if (!reversePriorityValue) it.first() else it.last() }
	}

    enum class AttackMode(override val choiceName: String) : NamedChoice {
        SINGLE("Single"),
        MULTI("Multi")
    }

    enum class RotationMode(override val choiceName: String) : NamedChoice {
        LOCK("Lock"),
        NONE("None")
    }

	enum class PriorityMode(override val choiceName: String) : NamedChoice {
		DISTANCE("Distance"),
		HEALTH("Health"),
		DIRECTION("Direction")
	}
}
