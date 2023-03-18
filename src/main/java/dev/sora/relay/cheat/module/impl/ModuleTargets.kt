package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.entity.EntityUnknown

class ModuleTargets : CheatModule("Targets") {

	private var targetPlayersValue by boolValue("TargetPlayers", true)
	private var targetEntitiesValue by boolValue("TargetEntities", false)
    private var antiBotModeValue by listValue("AntiBotMode", AntiBotMode.values(), AntiBotMode.NONE)

	fun Entity.isTarget(): Boolean {
		return if (this == session.thePlayer) false
		else if (targetPlayersValue && this is EntityPlayer) !this.isBot()
		else if (targetEntitiesValue && this is EntityUnknown) true
		else false
	}

    fun EntityPlayer.isBot(): Boolean {
        if (this is EntityPlayerSP || !state) return false

        return when (antiBotModeValue) {
            AntiBotMode.PLAYER_LIST -> {
                val playerList = session.theWorld.playerList[this.uuid] ?: return true
                playerList.name.isBlank()
            }
			AntiBotMode.NONE -> false
        }
    }

    enum class AntiBotMode(override val choiceName: String) : NamedChoice {
        PLAYER_LIST("PlayerList"),
		NONE("None")
    }
}
