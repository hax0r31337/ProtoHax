package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.sora.relay.utils.timing.ClickTimer

open class IntRangeValue(name: String, value: IntRange, val range: IntRange) : Value<IntRange>(name, value) {

	override fun toJson() = JsonObject().apply {
		addProperty("min", value.first)
		addProperty("max", value.last)
	}

	override fun fromJson(element: JsonElement) {
		if (element.isJsonObject) {
			val obj = element.asJsonObject
			if (obj.has("min") && obj.has("max")) {
				value = IntRange(obj.get("min").asInt, obj.get("max").asInt)
			}
		}
	}

	override fun fromString(newValue: String) {
		val array = if (newValue.contains("..")) {
			newValue.split("..")
		} else if (newValue.contains(' ')) {
			newValue.split(' ')
		} else {
			throw IllegalArgumentException("Unable to parse value: $newValue")
		}

		val num1 = array[0].toInt()
		val num2 = array[1].toInt()
		value = IntRange(Math.min(num1, num2), Math.max(num1, num2))
	}
}

class ClickValue(name: String = "CPS", value: IntRange = 5..7, range: IntRange = 1..20)
		: IntRangeValue(name, value, range) {

	private val clickTimer = ClickTimer()

	val canClick: Boolean
		get() = clickTimer.canClick()

	fun click() {
		clickTimer.update(value.first, value.last)
	}
}
