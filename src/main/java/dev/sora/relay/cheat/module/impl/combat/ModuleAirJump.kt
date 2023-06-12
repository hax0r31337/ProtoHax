package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class ModuleAirJump : CheatModule("AirJump", CheatCategory.COMBAT) {

	private var speedMultiplierValue by floatValue("SpeedMultiplier", 1f, 0.5f..3f)

	private val onTick = handleOneTime<EventTick>({ it.session.thePlayer.inputData.contains(PlayerAuthInputData.JUMP_DOWN) }) {
		val player = it.session.thePlayer
		if (!player.onGround && !player.prevOnGround) {
			it.session.netSession.inboundPacket(SetEntityMotionPacket().apply {
				runtimeEntityId = player.runtimeEntityId
				motion = Vector3f.from(player.motionX * speedMultiplierValue, 0.42f, player.motionZ * speedMultiplierValue)
			})
		}
	}
}
