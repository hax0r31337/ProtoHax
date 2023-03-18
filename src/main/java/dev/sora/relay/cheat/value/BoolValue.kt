package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

/**
 * Bool value represents a value with a boolean
 */
open class BoolValue(name: String, value: Boolean) : Value<Boolean>(name, value) {

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            value = element.asBoolean || element.asString.equals("true", ignoreCase = true)
        }
    }

    override fun fromString(newValue: String) {
        value = when (newValue.lowercase()) {
            "on", "true" -> true
            "off", "false" -> false
            "!", "rev", "reverse" -> !value
            else -> value
        }
    }
}