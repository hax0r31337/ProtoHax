package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.EventPacketOutbound
import org.cloudburstmc.protocol.bedrock.data.LevelEvent
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket

class ModuleHitEffect : CheatModule("HitEffect") {

	private var effectValue by listValue("Effect", Effect.values(), Effect.CRITICAL)

	private val onPacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet

		if (packet is InventoryTransactionPacket && packet.transactionType == InventoryTransactionType.ITEM_USE_ON_ENTITY && packet.actionType == 1) {
			effectValue.deployEffect(event.session, event.session.theWorld.entityMap[packet.runtimeEntityId] ?: return@handle)
		}
	}

	enum class Effect(override val choiceName: String) : NamedChoice {
		CRITICAL("Critical") {
			override fun deployEffect(session: GameSession, target: Entity) {
				session.netSession.inboundPacket(AnimatePacket().apply {
					runtimeEntityId = target.runtimeEntityId
					action = AnimatePacket.Action.CRITICAL_HIT
				})
			}
		},
		BLOOD("Blood") {
			override fun deployEffect(session: GameSession, target: Entity) {
				session.netSession.inboundPacket(LevelEventPacket().apply {
					type = LevelEvent.PARTICLE_DESTROY_BLOCK
					position = if (target is EntityPlayer) target.vec3PositionFeet.add(0f, 1f, 0f) else target.vec3Position
					data = session.blockMapping.getRuntimeByIdentifier("minecraft:redstone_block")
				})
			}
		},
		LAVA("Lava") {
			override fun deployEffect(session: GameSession, target: Entity) {
				session.netSession.inboundPacket(LevelEventPacket().apply {
					type = LevelEvent.PARTICLE_DESTROY_BLOCK
					position = if (target is EntityPlayer) target.vec3PositionFeet.add(0f, 1f, 0f) else target.vec3Position
					data = session.blockMapping.getRuntimeByIdentifier("minecraft:lava[liquid_depth=15]")
				})
			}
		};

		abstract fun deployEffect(session: GameSession, target: Entity)
	}
}
