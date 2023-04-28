package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.utils.toRotation
import dev.sora.relay.utils.timing.ClickTimer
import kotlin.math.pow

class ModuleKillAura : CheatModule("KillAura") {

    private var cpsValue by intValue("CPS", 7, 1..20)
    private var rangeValue by floatValue("Range", 3.7f, 2f..7f)
    private var attackModeValue by listValue("AttackMode", AttackMode.values(), AttackMode.SINGLE)
    private var rotationModeValue by listValue("RotationMode", RotationMode.values(), RotationMode.LOCK)
    private var swingValue by listValue("Swing", EntityPlayerSP.SwingMode.values(), EntityPlayerSP.SwingMode.BOTH)
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
			entityList.first()
		} else {
			when(attackModeValue) {
				AttackMode.MULTI -> {
					entityList.forEach { session.thePlayer.attackEntity(it, swingValue, swingSoundValue) }
					entityList.first()
				}
				AttackMode.SINGLE -> (entityList.minByOrNull { it.distanceSq(event.session.thePlayer) } ?: return@handle).also {
					session.thePlayer.attackEntity(it, swingValue, swingSoundValue)
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

    enum class AttackMode(override val choiceName: String) : NamedChoice {
        SINGLE("Single"),
        MULTI("Multi")
    }

    enum class RotationMode(override val choiceName: String) : NamedChoice {
        LOCK("Lock"),
        NONE("None")
    }
}
