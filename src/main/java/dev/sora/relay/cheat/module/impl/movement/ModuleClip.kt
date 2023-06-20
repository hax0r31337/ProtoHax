package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import kotlin.math.cos
import kotlin.math.sin

class ModuleClip : CheatModule("Clip", CheatCategory.MOVEMENT, canToggle = false) {

	private var verticalValue by floatValue("Vertical", 3f, -10f..10f)
	private var horizontalValue by floatValue("Horizontal", 3f, -10f..10f)

	override fun onEnable() {
		if (!session.netSessionInitialized) return

		val player = session.player
		val yaw = Math.toRadians(player.rotationYaw.toDouble()).toFloat()

		player.teleport(player.posX - sin(yaw) * horizontalValue, player.posY + verticalValue, player.posZ + cos(yaw) * horizontalValue)
	}
}
