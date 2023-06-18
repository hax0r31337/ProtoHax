package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import dev.sora.relay.utils.logWarn
import kotlin.reflect.KProperty

typealias ValueListener<T> = (T) -> T

abstract class Value<T>(val name: String, valueIn: T) {

    private val listeners = mutableListOf<ValueListener<T>>()

    var value: T = valueIn
        set(newValue) {
            if (newValue == field) return
            if (!validateValue(newValue)) {
                logWarn("[ValueSystem ($name)]: failed to validate value $newValue")
                return
            }

            val oldValue = field
            var currValue = newValue

            try {
                listeners.forEach {
                    currValue = it(currValue)
                }
                field = currValue
            } catch (e: Exception) {
                logWarn("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
            }
        }

    val defaultValue = valueIn

	private var visibleCheck: () -> Boolean = { true }

	val visible: Boolean
		get() = visibleCheck()

	var isVisibilityVariable = false
		private set

	fun visible(func: () -> Boolean): Value<T> {
		if (!isVisibilityVariable) {
			visibleCheck = func
			isVisibilityVariable = true
		} else {
			val oldFunc = visibleCheck
			visibleCheck = {
				oldFunc() && func()
			}
		}
		return this
	}

    open fun reset() {
        value = defaultValue
    }

    open fun validateValue(value: T): Boolean = true

	fun listen(listener: ValueListener<T>): Value<T> {
		listeners.add(listener)
		return this
	}

    abstract fun fromString(newValue: String)

    abstract fun toJson(): JsonElement?
    abstract fun fromJson(element: JsonElement)

    operator fun getValue(from: Any, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(from: Any, property: KProperty<*>, newValue: T) {
        value = newValue
    }
}
