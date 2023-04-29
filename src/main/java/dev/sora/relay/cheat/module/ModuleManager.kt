package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.module.impl.*
import dev.sora.relay.game.GameSession

class ModuleManager(private val session: GameSession) {

    val modules = mutableListOf<CheatModule>()

    fun registerModule(module: CheatModule) {
		module.session = session
		module.moduleManager = this
        modules.add(module)
        module.register(session.eventManager)
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
        registerModule(ModuleDeviceSpoof())
        registerModule(ModuleResourcePackSpoof())
        registerModule(ModuleTargets())
        registerModule(ModuleNoFall())
        registerModule(ModuleAntiBlind())
        registerModule(ModuleFastBreak())
        registerModule(ModuleBlink())
        registerModule(ModuleBlockFly())
        registerModule(ModuleInventoryHelper())
		registerModule(ModuleAirJump())
		registerModule(ModuleClip())
    }

	inline fun <reified T : CheatModule> getModule(t: Class<T>): T {
		return modules.filterIsInstance<T>().first()
	}
}
