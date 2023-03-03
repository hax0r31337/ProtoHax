package dev.sora.relay.cheat.command.impl

import dev.sora.relay.cheat.command.Command
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.game.GameSession

class CommandToggle(private val moduleManager: ModuleManager) : Command("toggle", "t") {

    override fun exec(args: Array<String>, session: GameSession) {
        if (args.isEmpty()) {
            session.chat("-t <module>")
            return
        }
        val mod = moduleManager.modules.find { it.name.equals(args[0], ignoreCase = true) }
        if (mod == null) {
            session.chat("no such module called ${args[0]} found")
            return
        }
        mod.state = !mod.state
        session.chat("module ${mod.name} toggled ${if (mod.state) "on" else "off"}")
    }
}
