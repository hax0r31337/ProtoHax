package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.event.Listener

abstract class CheatModule(val name: String,
                           defaultOn: Boolean = false,
                           private val canToggle: Boolean = true) : BasicThing(), Listener {

    var state = defaultOn
        set(state) {
            if (field == state) return

            if (!canToggle) {
                onEnable()
                return
            }
            field = state

            if (state) {
                onEnable()
            } else {
                onDisable()
            }
        }

    open fun onEnable() {}

    open fun onDisable() {}

    override fun listen() = state
}