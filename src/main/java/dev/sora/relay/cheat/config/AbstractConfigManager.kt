package dev.sora.relay.cheat.config

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.utils.logError
import java.io.Reader

abstract class AbstractConfigManager(val moduleManager: ModuleManager) {

    abstract fun listConfig(): List<String>

    protected abstract fun loadConfigData(name: String): Reader

    protected abstract fun saveConfigData(name: String, data: ByteArray)

    fun loadConfig(name: String) {
        try {
            val json = JsonParser.parseReader(loadConfigData(name)).asJsonObject
            loadConfigSectionModule(json.getAsJsonObject("modules"))
        } catch (t: Throwable) {
            logError("failed to load config", t)
        }
    }

    fun saveConfig(name: String) {
        try {
            val json = JsonObject()

            json.add("modules", saveConfigSectionModule())

            saveConfigData(name, Gson().toJson(json).toByteArray(Charsets.UTF_8))
        } catch (t: Throwable) {
            logError("failed to save config", t)
        }
    }

    protected fun loadConfigSectionModule(json: JsonObject) {
        moduleManager.modules.forEach {
            if (!json.has(it.name)) return@forEach
            val moduleJson = json.getAsJsonObject(it.name)
            it.state = try {
                moduleJson.get("state").asBoolean
            } catch (t: Throwable) {
                it.defaultOn
            }

            if (!moduleJson.has("values")) return@forEach
            val valuesJson = moduleJson.getAsJsonObject("values")
            it.getValues().forEach { v ->
                if (!valuesJson.has(v.name)) return@forEach
                v.fromJson(valuesJson.get(v.name))
            }
        }
    }

    protected fun saveConfigSectionModule(): JsonObject {
        val json = JsonObject()

        moduleManager.modules.forEach {
            val moduleJson = JsonObject()
            moduleJson.addProperty("state", it.state)
            it.getValues().also { values ->
                if (values.isNotEmpty()) {
                    val valuesJson = JsonObject()
                    values.forEach { v ->
                        valuesJson.add(v.name, v.toJson())
                    }
                    moduleJson.add("values", valuesJson)
                }
            }
            json.add(it.name, moduleJson)
        }

        return json
    }
}