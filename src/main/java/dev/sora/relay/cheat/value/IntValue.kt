package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

/**
 * Integer value represents a value with a integer
 */
open class IntValue(name: String, value: Int, val range: IntRange) : Value<Int>(name, value) {

    fun set(newValue: Number) {
        value = newValue.toInt()
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            value = element.asInt
        }
    }

    override fun fromString(newValue: String) {
        value = newValue.toInt()
    }
}
