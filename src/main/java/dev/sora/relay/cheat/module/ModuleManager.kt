package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.module.impl.ModuleFly
import dev.sora.relay.cheat.module.impl.ModuleKillAura
import dev.sora.relay.cheat.module.impl.ModuleSpammer
import dev.sora.relay.cheat.module.impl.ModuleVelocity
import dev.sora.relay.game.GameSession

class ModuleManager(private val session: GameSession) {

    val modules = mutableListOf<CheatModule>()

    fun registerModule(module: CheatModule) {
        module.session = session
        modules.add(module)
        session.eventManager.registerListener(module)
    }

    fun init() {
        registerModule(ModuleFly())
        registerModule(ModuleVelocity())
        registerModule(ModuleKillAura())
        registerModule(ModuleSpammer())
    }
}