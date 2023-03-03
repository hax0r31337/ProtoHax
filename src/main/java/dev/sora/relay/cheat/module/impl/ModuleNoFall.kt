package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket


class ModuleNoFall : CheatModule("NoFall") {

    private var modeValue by listValue("Mode", Mode.values(), Mode.ON_GROUND)

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet

		if (modeValue == Mode.ON_GROUND) {
			if (packet is MovePlayerPacket) {
				packet.isOnGround = true
			}
		} else if (modeValue == Mode.NO_GROUND) {
			if (packet is MovePlayerPacket) {
				packet.isOnGround = false
			}
		}
	}

    private val handleTick = handle<EventTick> { event ->
		val session = event.session

		if (modeValue == Mode.ELYTRA_GLITCH) {
			session.sendPacket(PlayerActionPacket().apply {
				runtimeEntityId = session.thePlayer.entityId
				action = PlayerActionType.START_GLIDE
			})
		} else if (modeValue == Mode.CUBECRAFT) {
		}
	}

    enum class Mode(override val choiceName: String) : NamedChoice {
        ON_GROUND("OnGround"),
        NO_GROUND("NoGround"),
        ELYTRA_GLITCH("ElytraGlitch"),
        CUBECRAFT("CubeCraft")
    }
}
