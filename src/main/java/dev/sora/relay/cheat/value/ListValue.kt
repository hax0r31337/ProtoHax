package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

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

    override fun fromString(newValue: String) {
        value = values.find { it.choiceName.equals(newValue, true) } ?: defaultValue
    }

	fun roll() {
		val idx = values.indexOf(value) + 1
		value = if (idx == values.size) {
			values.first()
		} else values[idx]
	}
}

interface NamedChoice {
    val choiceName: String
}
