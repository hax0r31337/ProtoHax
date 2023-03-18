package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket


class ModuleNoFall : CheatModule("NoFall") {

    private var modeValue by choiceValue("Mode", arrayOf(OnGround, NoGround, ElytraGlitch, Cubecraft), OnGround)

	object OnGround : Choice("OnGround") {

		val handlePacketOutbound = handle<EventPacketOutbound> { event ->
			if (event.packet is MovePlayerPacket) {
				event.packet.isOnGround = true
			}
		}
	}

	object NoGround : Choice("NoGround") {

		val handlePacketOutbound = handle<EventPacketOutbound> { event ->
			if (event.packet is MovePlayerPacket) {
				event.packet.isOnGround = false
			}
		}
	}

	object ElytraGlitch : Choice("ElytraGlitch") {

		val handleTick = handle<EventTick> { event ->
			if (event.session.thePlayer.tickExists % 10 == 0L) {
				event.session.sendPacket(PlayerActionPacket().apply {
					runtimeEntityId = event.session.thePlayer.entityId
					action = PlayerActionType.START_GLIDE
				})
			}
		}
	}

	object Cubecraft : Choice("Cubecraft") {

		val handlePacketOutbound = handle<EventPacketOutbound> { event ->
			val packet = event.packet
			if (packet is PlayerAuthInputPacket) {
				if (packet.delta.y < -0.3f) {
					packet.delta = Vector3f.ZERO
				}
			}
		}
	}
}
