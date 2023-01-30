package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.cheat.value.Value
import dev.sora.relay.cheat.value.ValueHolder
import dev.sora.relay.game.event.Listener

abstract class CheatModule(val name: String,
                           val defaultOn: Boolean = false,
                           val canToggle: Boolean = true) : BasicThing(), Listener, ValueHolder {

    override val values = mutableListOf<Value<*>>()

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

    open fun toggle() {
        this.state = !this.state
    }

    override fun listen() = state
}