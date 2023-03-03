package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.util.*

/**
 * List value represents a selectable list of values
 */
open class ListValue<T : NamedChoice>(name: String, val values: Array<T>, value: T) : Value<T>(name, value) {

    operator fun contains(value: T): Boolean {
        return values.contains(value)
    }

    override fun toJson() = JsonPrimitive(value.choiceName)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            fromString(element.asString)
        }
    }

    fun fromString(valueName: String) {
        value = values.find { it.choiceName.equals(valueName, true) } ?: defaultValue
    }

    fun hasValue(valueName: String)
        = values.any { it.choiceName.equals(valueName, true) }
}

interface NamedChoice {
    val choiceName: String
}