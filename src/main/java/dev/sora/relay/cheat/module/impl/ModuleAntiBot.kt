package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventManager

object ModuleAntiBot : CheatModule("AntiBot") {

    private var modeValue by listValue("Mode", Mode.values(), Mode.PLAYER_LIST)

    fun EntityPlayer.isBot(session: GameSession): Boolean {
        if (this is EntityPlayerSP || !state) return false

        return when (modeValue) {
            Mode.PLAYER_LIST -> {
                val playerList = session.theWorld.playerList[this.uuid] ?: return true
                playerList.name.isBlank()
            }
        }
    }

    enum class Mode(override val choiceName: String) : NamedChoice {
        PLAYER_LIST("PlayerList")
    }
}
