package dev.sora.relay.cheat.module.impl.combat

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityLocalPlayer
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityUnknown
import dev.sora.relay.game.event.*
import dev.sora.relay.utils.analyzeColorCoverage
import dev.sora.relay.utils.timing.MillisecondTimer
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket

class ModuleTargets : CheatModule("Targets", CheatCategory.COMBAT, canToggle = false) {

	private var targetPlayersValue by boolValue("TargetPlayers", true)
	private var targetEntitiesValue by boolValue("TargetEntities", false)
    private var antiBotModeValue by listValue("AntiBotMode", AntiBotMode.values(), AntiBotMode.NONE)
	private var teamCheckModeValue by listValue("TeamCheckMode", TeamCheckMode.values(), TeamCheckMode.NONE)

	private var attackTimer = MillisecondTimer()
	var previousAttack: Entity? = null
		get() {
			if (attackTimer.hasTimePassed(3000)) {
				field = null
			}

			return field
		}
		private set

	fun Entity.isTarget(): Boolean {
		return if (this == session.player) false
		else if (targetPlayersValue && this is EntityPlayer) !this.isBot() && !this.isTeammate()
		else if (targetEntitiesValue && this is EntityUnknown) true
		else false
	}

    fun EntityPlayer.isBot(): Boolean {
        if (this is EntityLocalPlayer) return false

        return when (antiBotModeValue) {
            AntiBotMode.PLAYER_LIST -> {
                val playerList = session.level.playerList[this.uuid] ?: return true
                playerList.name.isBlank()
            }
			AntiBotMode.NONE -> false
        }
    }

	fun EntityPlayer.isTeammate(): Boolean {
		if (this is EntityLocalPlayer) return false

		return when (teamCheckModeValue) {
			TeamCheckMode.NAME_TAG -> {
				val selfColor = session.player.displayName.let { analyzeColorCoverage(it).maxBy { it.value }.key }
				val playerColor = this.displayName.let { analyzeColorCoverage(it).maxBy { it.value }.key }

				selfColor == playerColor
			}
			TeamCheckMode.NONE -> false
		}
	}

	private val handlePacketOutbound = handleConstant<EventPacketOutbound> {
		if (packet is InventoryTransactionPacket && packet.transactionType == InventoryTransactionType.ITEM_USE_ON_ENTITY && packet.actionType == 1) {
			val entity = session.level.entityMap[packet.runtimeEntityId] ?: return@handleConstant
			if (!entity.isTarget()) {
				return@handleConstant
			}
			if (previousAttack != entity) {
				session.eventManager.emit(EventTargetChange(session, entity))
			}
			previousAttack = entity
			attackTimer.reset()
		}
	}

	private val handleEntityDespawn = handleConstant<EventEntityDespawn> {
		if (previousAttack != null && entity == previousAttack && !attackTimer.hasTimePassed(3000)) {
			previousAttack = null
			session.eventManager.emit(EventTargetKilled(session, entity))
		}
	}

	@Suppress("unchecked_cast")
	protected inline fun <reified T : GameEvent> handleConstant(noinline handler: Handler<T>) {
		handlers.add(EventHook(T::class.java, handler) as EventHook<in GameEvent>)
	}

	class EventTargetChange(session: GameSession, val target: Entity) : GameEvent(session, "target_changed")

	class EventTargetKilled(session: GameSession, val target: Entity) : GameEvent(session, "target_killed")

	private enum class AntiBotMode(override val choiceName: String) : NamedChoice {
        PLAYER_LIST("PlayerList"),
		NONE("None")
    }

	private enum class TeamCheckMode(override val choiceName: String) : NamedChoice {
		NAME_TAG("NameTag"),
		NONE("None")
	}
}
