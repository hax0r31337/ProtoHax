package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.module.impl.*
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
        registerModule(ModuleBGM())
        registerModule(ModuleDisabler())
        registerModule(ModuleOpFightBot())
        registerModule(ModuleNoSkin())
        registerModule(ModuleLoginIDSpoof())
        registerModule(ModuleResourcePackSpoof())
        registerModule(ModuleAntiBot)
        registerModule(ModuleEditionFaker())
        registerModule(ModuleNoFall())
        registerModule(ModuleAntiKick())
        registerModule(ModuleAutoSprint())
        registerModule(ModuleNightVision())
        registerModule(ModuleAntiDebuff())
    }
}