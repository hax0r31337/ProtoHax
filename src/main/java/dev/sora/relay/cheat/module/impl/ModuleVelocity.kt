package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class ModuleVelocity : CheatModule("Velocity") {

    private var modeValue by choiceValue("Mode", arrayOf(Vanilla, Simple), Vanilla)

	private object Vanilla : Choice("Vanilla") {

		private val handlePacketInbound = handle<EventPacketInbound> { event ->
			if (event.packet is SetEntityMotionPacket)
				event.cancel()
		}
	}

	private object Simple : Choice("Simple") {

		private var horizontalValue by floatValue("Horizontal", 0f, 0f..1f)
		private var verticalValue by floatValue("Vertical", 0f, 0f..1f)

		private val handlePacketInbound = handle<EventPacketInbound> { event ->
			if (event.packet is SetEntityMotionPacket)
				event.packet.motion = event.packet.motion.mul(horizontalValue, verticalValue, horizontalValue)
		}
	}
}
