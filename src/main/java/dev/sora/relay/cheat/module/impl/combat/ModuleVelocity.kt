package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class ModuleVelocity : CheatModule("Velocity", CheatCategory.COMBAT) {

    private var modeValue by choiceValue("Mode", arrayOf(Vanilla, Simple), Vanilla)

	private object Vanilla : Choice("Vanilla") {

		private val handlePacketInbound = handle<EventPacketInbound> {
			if (packet is SetEntityMotionPacket) {
				cancel()
			}
		}
	}

	private object Simple : Choice("Simple") {

		private var horizontalValue by floatValue("Horizontal", 0f, 0f..1f)
		private var verticalValue by floatValue("Vertical", 0f, 0f..1f)

		private val handlePacketInbound = handle<EventPacketInbound> {
			if (packet is SetEntityMotionPacket) {
				packet.motion = packet.motion.mul(horizontalValue, verticalValue, horizontalValue)
			}
		}
	}
}
