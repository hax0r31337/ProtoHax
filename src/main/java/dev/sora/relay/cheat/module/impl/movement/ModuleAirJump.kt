package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class ModuleAirJump : CheatModule("AirJump", CheatCategory.MOVEMENT) {

	private var speedMultiplierValue by floatValue("SpeedMultiplier", 1f, 0.5f..3f)

	private val onTick = handleOneTime<EventTick>({ it.session.player.inputData.contains(PlayerAuthInputData.JUMP_DOWN) }) {
		val player = session.player

		if (!player.onGround && !player.prevOnGround) {
			session.netSession.inboundPacket(SetEntityMotionPacket().apply {
				runtimeEntityId = player.runtimeEntityId
				motion = Vector3f.from(player.motionX * speedMultiplierValue, 0.42f, player.motionZ * speedMultiplierValue)
			})
		}
	}
}
