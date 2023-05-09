package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket

class ModuleNoHurtCam : CheatModule("NoHurtCam") {

	private val handlePacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if (packet is EntityEventPacket && packet.runtimeEntityId == event.session.thePlayer.runtimeEntityId
			&& packet.type == EntityEventType.HURT) {

			event.cancel()
		}
	}
}
