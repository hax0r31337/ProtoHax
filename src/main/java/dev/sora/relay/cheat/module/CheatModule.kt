package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.event.Listener
import dev.sora.relay.cheat.value.Value

abstract class CheatModule(val name: String,
                           val defaultOn: Boolean = false,
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

    fun getValues() = javaClass.declaredFields.map { field ->
        field.isAccessible = true
        field.get(this)
    }.filterIsInstance<Value<*>>()

    fun getValue(valueName: String) = this.getValues().find { it.name.equals(valueName, ignoreCase = true) }

    override fun listen() = state
}