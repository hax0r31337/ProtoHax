package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.event.EventTick
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ModuleOpFightBot : CheatModule("OPFightBot") {

    private var modeValue by listValue("Mode", Mode.values(), Mode.STRAFE)
    private var rangeValue by floatValue("Range", 1.5f, 1.5f..4f)
	private var passiveValue by boolValue("Passive", false)
    private var horizontalSpeedValue by floatValue("HorizontalSpeed", 5f, 1f..7f)
    private var verticalSpeedValue by floatValue("VerticalSpeed", 4f, 1f..7f)
    private var strafeSpeedValue by intValue("StrafeSpeed", 20, 10..90)

	private val handleTick = handle<EventTick> { event ->
		val session = event.session
		val moduleTargets = moduleManager.getModule(ModuleTargets::class.java)
		val target = session.theWorld.entityMap.values.filter { with(moduleTargets) { it.isTarget() } }
			.minByOrNull { it.distanceSq(session.thePlayer) } ?: return@handle
		if(target.distance(session.thePlayer) < 5) {
			val direction = Math.toRadians(when(modeValue) {
				Mode.RANDOM -> Math.random() * 360
				Mode.STRAFE -> ((session.thePlayer.tickExists * strafeSpeedValue) % 360).toDouble()
				Mode.BEHIND -> target.rotationYaw + 180.0
			}).toFloat()
			session.thePlayer.teleport(target.posX - sin(direction) * rangeValue, target.posY + 0.5f, target.posZ + cos(direction) * rangeValue)
		} else if (!passiveValue) {
			val direction = atan2(target.posZ - session.thePlayer.posZ, target.posX - session.thePlayer.posX) - Math.toRadians(90.0).toFloat()
			session.thePlayer.teleport(session.thePlayer.posX - sin(direction) * horizontalSpeedValue,
				target.posY.coerceIn(session.thePlayer.posY - verticalSpeedValue, session.thePlayer.posY + verticalSpeedValue),
				session.thePlayer.posZ + cos(direction) * horizontalSpeedValue)
		}
	}

	private enum class Mode(override val choiceName: String) : NamedChoice {
        RANDOM("Random"),
        STRAFE("Strafe"),
        BEHIND("Behind")
    }
}
