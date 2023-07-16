package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket

class ModuleHitbox : CheatModule("Hitbox", CheatCategory.COMBAT) {

	private var scaleValue by floatValue("Scale", 1.5f, 0.5f..5f)

	private val handleTick = handle<EventTick> {
		if (session.player.tickExists % 40 != 0L) {
			return@handle
		}

		val moduleTargets = moduleManager.getModule(ModuleTargets::class.java)

		session.level.entityMap.values.forEach { e ->
			if (with(moduleTargets) { !e.isTarget() }) {
				return@forEach
			}

			val meta = e.metadata[EntityDataTypes.SCALE] as? Float? ?: 1f
			if (meta != scaleValue) {
				e.metadata[EntityDataTypes.SCALE] = scaleValue
				session.sendPacketToClient(SetEntityDataPacket().apply {
					runtimeEntityId = e.runtimeEntityId
					this.metadata[EntityDataTypes.SCALE] = scaleValue
				})
			}
		}
	}
}
