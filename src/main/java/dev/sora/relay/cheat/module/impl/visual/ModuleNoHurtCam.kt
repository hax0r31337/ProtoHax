package dev.sora.relay.cheat.module.impl.visual

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket

class ModuleNoHurtCam : CheatModule("NoHurtCam", CheatCategory.VISUAL) {

	private val handlePacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if (packet is EntityEventPacket && packet.runtimeEntityId == event.session.player.runtimeEntityId
			&& packet.type == EntityEventType.HURT) {

			event.cancel()
		}
	}
}
