package dev.sora.relay.cheat.module.impl.misc

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class ModuleDisabler : CheatModule("Disabler", CheatCategory.MISC) {

    private var modeValue by choiceValue("Mode", arrayOf(Mineplex, Cubecraft, LifeBoat), Mineplex)

	private object Mineplex : Choice("Mineplex") {

		private val handleTick = handle<EventTick> {
			session.sendPacket(MovePlayerPacket().apply {
				val player = session.player
				runtimeEntityId = player.runtimeEntityId
				position = player.vec3Position
				rotation = player.vec3Rotation
				isOnGround = true
			})
		}
	}

	private object Cubecraft : Choice("Cubecraft") {

		private val handlePacketOutbound = handle<EventPacketOutbound> {
			if (packet is PlayerAuthInputPacket) {
				packet.motion = Vector2f.from(0.01f, 0.01f)

				for (i in 0 until 9) {
					session.netSession.outboundPacket(packet)
				}
			} else if (packet is NetworkStackLatencyPacket) {
				cancel()
			}
		}

		private val handleTick = handle<EventTick> {
			session.sendPacket(MovePlayerPacket().apply {
				val player = session.player
				runtimeEntityId = player.runtimeEntityId
				position = player.vec3Position
				rotation = player.vec3Rotation
				isOnGround = true
			})
		}
	}

	private object LifeBoat : Choice("LifeBoat") {

		private val handlePacketOutbound = handle<EventPacketOutbound> {
			if (packet is PlayerAuthInputPacket) {
				session.sendPacket(MovePlayerPacket().apply {
					position = packet.position.add(0f, 0.1f, 0f)
					rotation = packet.rotation
					mode = MovePlayerPacket.Mode.NORMAL
					isOnGround = false
				})
			}
		}
	}
}
