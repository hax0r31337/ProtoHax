package dev.sora.relay.cheat

import dev.sora.relay.game.GameSession

class ModuleManager(private val session: GameSession) {

    val modules = mutableListOf<CheatModule>()

    private fun registerModule(module: CheatModule) {
        module.session = session
        modules.add(module)
        session.eventManager.registerListener(module)
    }

    fun init() {
        // TODO: register modules
    }
}