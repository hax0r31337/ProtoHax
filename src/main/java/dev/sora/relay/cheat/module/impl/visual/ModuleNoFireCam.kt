package dev.sora.relay.cheat.module.impl.visual

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket

class ModuleNoFireCam : CheatModule("NoFireCam", CheatCategory.VISUAL) {

	private val handlePacketInbound = handle<EventPacketInbound> {
		if (packet is SetEntityDataPacket) {
			if(packet.runtimeEntityId == session.player.runtimeEntityId){
				packet.metadata.setFlag(EntityFlag.ON_FIRE,false)
			}
		}
	}
}
