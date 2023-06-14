package dev.sora.relay.cheat.config.section

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.utils.asJsonObjectOrNull

class ConfigSectionModule(val moduleManager: ModuleManager) : IConfigSection {

	override val sectionName: String
		get() = "modules"

	override fun load(element: JsonElement?) {
		val json = element?.asJsonObjectOrNull ?: JsonObject()

		moduleManager.modules.forEach {
			val moduleJson = json.get(it.name)?.asJsonObjectOrNull ?: JsonObject()
			it.state = moduleJson.get("state")?.let { v ->
				if (v.isJsonPrimitive) {
					val p = v.asJsonPrimitive
					if (p.isBoolean) {
						p.asBoolean
					} else {
						null
					}
				} else null
			} ?: it.defaultOn

			if (it.values.isNotEmpty()) {
				val valuesJson = moduleJson.get("values")?.asJsonObjectOrNull ?: JsonObject()
				it.values.forEach { v ->
					if (valuesJson.has(v.name)) {
						try {
						    v.fromJson(valuesJson.get(v.name))
						} catch (t: Throwable) {
							v.reset()
						}
					} else {
						v.reset()
					}
				}
			}
		}
	}

	override fun save(): JsonObject {
		val json = JsonObject()

		moduleManager.modules.forEach {
			val moduleJson = JsonObject()
			moduleJson.addProperty("state", it.state)
			if (it.values.isNotEmpty()) {
				val valuesJson = JsonObject()
				it.values.forEach { v ->
					valuesJson.add(v.name, v.toJson())
				}
				moduleJson.add("values", valuesJson)
			}
			json.add(it.name, moduleJson)
		}

		return json
	}
}
