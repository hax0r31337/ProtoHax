package dev.sora.relay.cheat.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.utils.logError
import java.io.Reader

abstract class AbstractConfigManager(val moduleManager: ModuleManager) {

    val DEFAULT_GSON = GsonBuilder().setPrettyPrinting().create()

    abstract fun listConfig(): List<String>

    protected abstract fun loadConfigData(name: String): Reader?

    protected abstract fun saveConfigData(name: String, data: ByteArray)

    abstract fun deleteConfig(name: String)

    /**
     * @return false if failed to load the config or config not exists
     */
    fun loadConfig(name: String): Boolean {
        try {
            val json = JsonParser.parseReader(loadConfigData(name) ?: return false).asJsonObject
            loadConfigSectionModule(json.getAsJsonObject("modules"))
            return true
        } catch (t: Throwable) {
            logError("failed to load config", t)
            return false
        }
    }

    fun saveConfig(name: String) {
        try {
            val json = JsonObject()

            json.add("modules", saveConfigSectionModule())

            saveConfigData(name, DEFAULT_GSON.toJson(json).toByteArray(Charsets.UTF_8))
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