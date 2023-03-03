package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.util.*

/**
 * List value represents a selectable list of values
 */
open class ListValue(name: String, val values: Array<String>, value: String) : Value<String>(name, value) {

    var openList = false

    init {
        this.value = value
    }

    operator fun contains(string: String?): Boolean {
        return Arrays.stream(values).anyMatch { s: String -> s.equals(string, ignoreCase = true) }
    }

    override fun validateValue(value: String): Boolean {
        for (element in values) {
            if (element.equals(value, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) value = element.asString
    }
}