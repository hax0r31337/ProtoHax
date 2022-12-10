package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import dev.sora.relay.utils.logWarn

abstract class Value<T>(val name: String, protected var value: T) {
    val defaultValue = value

    open fun set(newValue: T) {
        if (newValue == value) return

        val oldValue = get()

        try {
            onChange(oldValue, newValue)
            changeValue(newValue)
            onChanged(oldValue, newValue)
        } catch (e: Exception) {
            logWarn("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
        }
    }

    open fun get() = value

    open fun changeValue(value: T) {
        this.value = value
    }

    open fun reset() {
        value = defaultValue
    }

    abstract fun toJson(): JsonElement?
    abstract fun fromJson(element: JsonElement)

    protected open fun onChange(oldValue: T, newValue: T) {}
    protected open fun onChanged(oldValue: T, newValue: T) {}
}