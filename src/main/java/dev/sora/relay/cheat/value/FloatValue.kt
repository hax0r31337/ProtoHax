package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

/**
 * Float value represents a value with a float
 */
open class FloatValue(name: String, value: Float, val range: ClosedFloatingPointRange<Float>) : Value<Float>(name, value) {

    fun set(newValue: Number) {
		value = newValue.toFloat()
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            value = element.asFloat
        }
    }

    override fun fromString(newValue: String) {
        value = newValue.toFloat()
    }
}
