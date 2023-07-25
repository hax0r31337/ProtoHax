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

    private var modeValue by choiceValue("Mode", arrayOf(CubeCraft, Lifeboat), CubeCraft)

	/*
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
	 */
	// RIP Mineplex

	private object CubeCraft : Choice("CubeCraft") {

		private val handlePacketOutbound = handle<EventPacketOutbound> {
			if (packet is PlayerAuthInputPacket) {
				packet.delta = packet.delta.mul(0.0,1.0,0.0);

				session.sendPacket(MovePlayerPacket().apply {
					runtimeEntityId = session.player.runtimeEntityId
					isOnGround = true
					mode = MovePlayerPacket.Mode.TELEPORT
					teleportationCause = MovePlayerPacket.TeleportationCause.PROJECTILE
					tick = session.player.tickExists
				})
				/*
				for (i in 0 until 9) {
					session.netSession.outboundPacket(packet)
				}
				 */
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
				tick = session.player.tickExists
			})
		}
	}

	private object Lifeboat : Choice("Lifeboat") {

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
