package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP

object ModuleAntiBot : CheatModule("AntiBot") {

    private val modeValue = listValue("Mode", arrayOf("LifeBoat"), "LifeBoat")

    fun EntityPlayer.isBot(session: GameSession): Boolean {
        if (this is EntityPlayerSP || !state) return false

        if (modeValue.get() == "LifeBoat") {
            val playerList = session.theWorld.playerList[this.uuid] ?: return true
            return playerList.name.isBlank()
        }
        return false
    }
}