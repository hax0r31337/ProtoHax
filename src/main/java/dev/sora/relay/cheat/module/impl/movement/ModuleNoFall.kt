package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket


class ModuleNoFall : CheatModule("NoFall", CheatCategory.MOVEMENT) {

	private var modeValue by choiceValue("Mode", arrayOf(ElytraGlitch, Cubecraft), ElytraGlitch)

	private object ElytraGlitch : Choice("ElytraGlitch") {

		val handleTick = handle<EventTick> {
			if (session.player.tickExists % 10 == 0L) {
				session.sendPacket(PlayerActionPacket().apply {
					runtimeEntityId = session.player.runtimeEntityId
					action = PlayerActionType.START_GLIDE
					blockPosition = Vector3i.ZERO
					resultPosition = Vector3i.ZERO
				})
			}
		}
	}

	private object Cubecraft : Choice("Cubecraft") {

		val handlePacketOutbound = handle<EventPacketOutbound> {
			if (packet is PlayerAuthInputPacket) {
				if (packet.delta.y < -0.3f) {
					packet.delta = Vector3f.ZERO
				}
			}
		}
	}
}
