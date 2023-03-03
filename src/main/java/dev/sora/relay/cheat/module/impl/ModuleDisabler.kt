package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class ModuleDisabler : CheatModule("Disabler") {

    private var modeValue by listValue("Mode", Mode.values(), Mode.LIFEBOAT)

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet

		when (modeValue) {
			Mode.LIFEBOAT -> {
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

			Mode.CUBECRAFT -> {
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

			else -> {}
		}
	}

	private val handleTick = handle<EventTick> { event ->
		val session = event.session

		if (modeValue == Mode.MINEPLEX || modeValue == Mode.CUBECRAFT) {
			session.sendPacket(MovePlayerPacket().apply {
				runtimeEntityId = session.thePlayer.entityId
				position = session.thePlayer.vec3Position
				rotation = session.thePlayer.vec3Rotation
				isOnGround = true
			})
		}
	}

    enum class Mode(override val choiceName: String) : NamedChoice {
        MINEPLEX("Mineplex"),
        CUBECRAFT("CubeCraft"),
        LIFEBOAT("LifeBoat")
    }
}
