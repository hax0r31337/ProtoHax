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

    open fun reset() {
        value = defaultValue
    }

    open fun validateValue(value: T): Boolean = true

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