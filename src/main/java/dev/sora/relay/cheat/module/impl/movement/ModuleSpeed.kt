package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.entity.EntityLocalPlayer
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import kotlin.math.cos
import kotlin.math.sin

class ModuleSpeed : CheatModule("Speed", CheatCategory.MOVEMENT) {

	private var modeValue by choiceValue("Mode", arrayOf(Simple(), Strafe()), "Simple")
	private var sprintToMoveValue by boolValue("SprintToMove", true)
	private var speedValue by floatValue("Speed", 0.5f, 0.1f..5f)
	private var jumpValue by floatValue("Jump", 0.42f, 0.2f..1f)

	private val canMove: Boolean
		get() = !sprintToMoveValue || session.player.inputData.contains(PlayerAuthInputData.SPRINT_DOWN)

	private fun getMoveDirectionAngle(player: EntityLocalPlayer): Float {
		return Math.toRadians(player.rotationYaw.toDouble()).toFloat()
	}

	private inner class Simple : Choice("Simple") {

		private val onTick = handle<EventTick> { event ->
			val player = event.session.player

			if (canMove) {
				if (player.onGround || player.motionY == 0f || (player.motionY > player.prevMotionY && player.motionY < 0)) {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						val angle = getMoveDirectionAngle(player)
						motion = Vector3f.from(-sin(angle) * speedValue, jumpValue, cos(angle) * speedValue)
					})
				}
			}
		}
	}

	private inner class Strafe : Choice("Strafe") {

		private val onTick = handle<EventTick> { event ->
			val player = event.session.player

			if (canMove) {
				val angle = getMoveDirectionAngle(player)
				val motionX = -sin(angle) * speedValue
				val motionZ = cos(angle) * speedValue

				if (player.onGround || player.motionY == 0f || (player.motionY > player.prevMotionY && player.motionY < 0f)) {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						motion = Vector3f.from(motionX, jumpValue, motionZ)
					})
				} else {
					event.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						motion = Vector3f.from(motionX, (player.motionY - 0.1f) * 0.95f, motionZ)
					})
				}
			}
		}
	}
}
