package dev.sora.relay.game.management

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.event.*
import dev.sora.relay.utils.timing.MillisecondTimer
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket

class TargetManager(private val session: GameSession) : Listenable {

	private var attackTimer = MillisecondTimer()
	private var previousAttack: Entity? = null
		get() {
			if (attackTimer.hasTimePassed(3000)) {
				field = null
			}

			return null
		}

	private val handlePacketOutbound = handle<EventPacketOutbound> {
		if (packet is InventoryTransactionPacket && packet.transactionType == InventoryTransactionType.ITEM_USE_ON_ENTITY && packet.actionType == 1) {
			val entity = session.level.entityMap[packet.runtimeEntityId] ?: return@handle
			if (previousAttack != entity) {
				eventManager.emit(EventTargetChange(session, entity))
			}
			previousAttack = entity
			attackTimer.reset()
		}
	}

	private val handleEntityDespawn = handle<EventEntityDespawn> {
		if (previousAttack != null && entity == previousAttack && !attackTimer.hasTimePassed(3000)) {
			eventManager.emit(EventTargetKilled(session, entity))
		}
	}

	override val eventManager: EventManager
		get() = session.eventManager
}
