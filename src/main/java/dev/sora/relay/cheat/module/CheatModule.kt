package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.cheat.value.ChoiceValue
import dev.sora.relay.cheat.value.Value
import dev.sora.relay.cheat.value.Configurable
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*

abstract class CheatModule(val name: String,
                           val defaultOn: Boolean = false,
                           val canToggle: Boolean = true) : Configurable {

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

	protected val handlers = mutableListOf<EventHook<in GameEvent>>()
	lateinit var session: GameSession

    open fun onEnable() {
		this.values.forEach {
			if (it is ChoiceValue) it.active()
		}
	}

    open fun onDisable() {
		this.values.forEach {
			if (it is ChoiceValue) it.inactive()
		}
	}

    open fun toggle() {
        this.state = !this.state
    }

	protected inline fun <reified T : GameEvent> handle(noinline handler: Handler<T>) {
		handlers.add(EventHook(T::class.java, handler, this::state) as EventHook<in GameEvent>)
	}

	protected inline fun <reified T : GameEvent> handle(crossinline condition: () -> Boolean, noinline handler: Handler<T>) {
		handlers.add(EventHook(T::class.java, handler) { this.state && condition() } as EventHook<in GameEvent>)
	}

	fun register(eventManager: EventManager) {
		handlers.forEach(eventManager::register)
	}

	fun choiceValue(name: String, values: Array<Choice>, defaultValue: Choice): ChoiceValue {
		val value = ChoiceValue(name, values, defaultValue)
		this.values.add(value)
		values.forEach {
			handlers.addAll(it.getHandlers(this))
			this.values.addAll(it.values)
		}

		return value
	}

	fun choiceValue(name: String, values: Array<Choice>, defaultValue: String)
		= choiceValue(name, values, values.find { it.choiceName == defaultValue }
				?: error("no such choice \"$defaultValue\" found in value \"$name\""))
}
