package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class ModuleDisabler : CheatModule("Disabler") {

    private var modeValue by choiceValue("Mode", arrayOf(Mineplex, Cubecraft, LifeBoat), Mineplex)

	object Mineplex : Choice("Mineplex") {

		private val handleTick = handle<EventTick> { event ->
			event.session.sendPacket(MovePlayerPacket().apply {
				val thePlayer = event.session.thePlayer
				runtimeEntityId = thePlayer.entityId
				position = thePlayer.vec3Position
				rotation = thePlayer.vec3Rotation
				isOnGround = true
			})
		}
	}

	object Cubecraft : Choice("Cubecraft") {

		private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
			val packet = event.packet

			if (packet is MovePlayerPacket) {
				for (i in 0 until 9) {
					event.session.netSession.outboundPacket(packet)
				}
			} else if (packet is PlayerAuthInputPacket) {
				packet.motion = Vector2f.from(0.01f, 0.01f)

				for (i in 0 until 9) {
					event.session.netSession.outboundPacket(packet)
				}
			} else if (packet is NetworkStackLatencyPacket) {
				event.cancel()
			}
		}

		private val handleTick = handle<EventTick> { event ->
			event.session.sendPacket(MovePlayerPacket().apply {
				val thePlayer = event.session.thePlayer
				runtimeEntityId = thePlayer.entityId
				position = thePlayer.vec3Position
				rotation = thePlayer.vec3Rotation
				isOnGround = true
			})
		}
	}

	object LifeBoat : Choice("LifeBoat") {

		private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
			val packet = event.packet

			if (packet is MovePlayerPacket) {
				packet.isOnGround = true
				event.session.netSession.outboundPacket(MovePlayerPacket().apply {
					runtimeEntityId = packet.runtimeEntityId
					position = packet.position.add(0f, 0.1f, 0f)
					rotation = packet.rotation
					mode = packet.mode
					isOnGround = false
				})
			}
		}
	}
}
