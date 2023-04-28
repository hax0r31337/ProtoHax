package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class ModuleAirJump : CheatModule("AirJump") {

	private var jumpPressed = false

	override fun onDisable() {
		jumpPressed = false
	}

	private val onTick = handle<EventTick> {
		if (it.session.thePlayer.inputData.contains(PlayerAuthInputData.JUMP_DOWN)) {
			if (!jumpPressed) {
				val player = it.session.thePlayer
				if (!player.onGround && !player.prevOnGround) {
					it.session.sendPacketToClient(SetEntityMotionPacket().apply {
						runtimeEntityId = player.entityId
						motion = Vector3f.from(player.posX - player.prevPosX, 0.42f, player.posZ - player.prevPosZ)
					})
				} else {
					println("skip")
				}
			}
			jumpPressed = true
		} else {
			jumpPressed = false
		}
	}
}
