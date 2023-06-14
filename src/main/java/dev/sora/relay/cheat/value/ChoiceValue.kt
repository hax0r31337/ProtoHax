package dev.sora.relay.cheat.value

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventHook
import dev.sora.relay.game.event.GameEvent
import dev.sora.relay.game.event.Handler

class ChoiceValue(name: String, values: Array<Choice>, value: Choice) : ListValue<Choice>(name, values, value) {

	private var isActive = false

	init {
	    listen { newValue ->
			if (isActive) {
				values.forEach {
					it.isActive = newValue == it
				}
			}
			newValue
		}
	}

	fun active() {
		isActive = true
		value.isActive = true
	}

	fun inactive() {
		isActive = false
		value.isActive = false
	}
}

abstract class Choice(override val choiceName: String) : NamedChoice, Configurable {

	open var isActive = false
		set(value) {
			if (value == field) return

			if (value) {
				onEnable()
			} else {
				onDisable()
			}
			field = value
		}
	val handlers = mutableListOf<Pair<Class<out GameEvent>, Handler<GameEvent>>>()

	override val values = mutableListOf<Value<*>>()

	open fun onEnable() {}

	open fun onDisable() {}

	@Suppress("unchecked_cast")
	protected inline fun <reified T : GameEvent> handle(noinline handler: Handler<T>) {
		handlers.add(T::class.java to handler as Handler<GameEvent>)
	}

	@Suppress("unchecked_cast")
	fun getHandlers(module: CheatModule): List<EventHook<in GameEvent>> {
		return handlers.map {
			EventHook(it.first, it.second) {
				module.state && isActive
			} as EventHook<in GameEvent>
		}
	}
}
