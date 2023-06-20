package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.entity.EntityLocalPlayer
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class ModuleSpeed : CheatModule("Speed", CheatCategory.MOVEMENT) {

	private var modeValue by choiceValue("Mode", arrayOf(Simple(), Strafe()), "Simple")
	private var speedValue by floatValue("Speed", 0.5f, 0.1f..5f)
	private var jumpValue by floatValue("Jump", 0.42f, 0.2f..1f)
	private var fakeSprintValue by boolValue("FakeSprint", false)

	private var sprinting = false

	override fun onDisable() {
		sprinting = false
	}

	private fun EntityLocalPlayer.stopFakeSprint() {
		sprinting = false
		inputData.add(PlayerAuthInputData.STOP_SPRINTING)
	}

	private fun EntityLocalPlayer.fakeSprint() {
		if (!sprinting) {
			inputData.add(PlayerAuthInputData.START_SPRINTING)
		}
		inputData.add(PlayerAuthInputData.SPRINT_DOWN)
		inputData.add(PlayerAuthInputData.SPRINTING)
		sprinting = true
	}

	private inner class Simple : Choice("Simple") {

		private val onTick = handle<EventTick> {
			val player = session.player

			val angle = player.moveDirectionAngle ?: run {
				if (fakeSprintValue) {
					player.stopFakeSprint()
				}
				return@handle
			}

			if (fakeSprintValue) {
				player.fakeSprint()
			}

			if (player.onGround || player.motionY == 0f || (player.motionY > player.prevMotionY && player.motionY < 0)) {
				session.netSession.inboundPacket(SetEntityMotionPacket().apply {
					runtimeEntityId = player.runtimeEntityId
					motion = Vector3f.from(-sin(angle) * speedValue, jumpValue, cos(angle) * speedValue)
				})
			}
		}
	}

	private inner class Strafe : Choice("Strafe") {

		private var resetMotionValue by boolValue("StrafeResetMotion", false)

		private val EntityLocalPlayer.nextMotionY: Float
			get() = (motionY - 0.1f) * 0.95f

		private val onTick = handle<EventTick> {
			val player = session.player

			val angle = player.moveDirectionAngle

			if (angle == null) {
				if (fakeSprintValue) {
					player.stopFakeSprint()
				}

				if (resetMotionValue && abs(player.motionX) > 0.01f && abs(player.motionZ) > 0.01f) {
					session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						motion = Vector3f.from(0f, player.nextMotionY, 0f)
					})
				}
			} else {
				if (fakeSprintValue) {
					player.fakeSprint()
				}
				val motionX = -sin(angle) * speedValue
				val motionZ = cos(angle) * speedValue

				if (player.onGround || player.motionY == 0f || (player.motionY > player.prevMotionY && player.motionY < 0f)) {
					session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						motion = Vector3f.from(motionX, jumpValue, motionZ)
					})
				} else {
					session.netSession.inboundPacket(SetEntityMotionPacket().apply {
						runtimeEntityId = player.runtimeEntityId
						motion = Vector3f.from(motionX, player.nextMotionY, motionZ)
					})
				}
			}
		}
	}
}
