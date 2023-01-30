package dev.sora.relay.cheat.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.utils.logError
import java.io.InputStream

abstract class AbstractConfigManager(val moduleManager: ModuleManager) {

    abstract fun listConfig(): List<String>

    protected abstract fun loadConfigData(name: String): InputStream?

    protected abstract fun saveConfigData(name: String, data: ByteArray)

    abstract fun deleteConfig(name: String): Boolean

    open fun copyConfig(src: String, dst: String): Boolean {
        val reader = loadConfigData(src) ?: return false
        saveConfigData(dst, reader.readBytes())
        return true
    }

    open fun renameConfig(src: String, dst: String): Boolean {
        if (!copyConfig(src, dst)) return false
        return deleteConfig(dst)
    }

    /**
     * @return false if failed to load the config or config not exists
     */
    open fun loadConfig(name: String): Boolean {
        try {
            val json = JsonParser.parseReader((loadConfigData(name) ?: return false).reader(Charsets.UTF_8)).asJsonObject
            loadConfigSectionModule(json.getAsJsonObject("modules"))
            return true
        } catch (t: Throwable) {
            logError("failed to load config", t)
            return false
        }
    }

    open fun saveConfig(name: String) {
        try {
            val json = JsonObject()

            json.add("modules", saveConfigSectionModule())

            saveConfigData(name, DEFAULT_GSON.toJson(json).toByteArray(Charsets.UTF_8))
        } catch (t: Throwable) {
            logError("failed to save config", t)
        }
    }

    protected open fun loadConfigSectionModule(json: JsonObject) {
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
            it.values.forEach { v ->
                if (!valuesJson.has(v.name)) return@forEach
                v.fromJson(valuesJson.get(v.name))
            }
        }
    }

    protected open fun saveConfigSectionModule(): JsonObject {
        val json = JsonObject()

        moduleManager.modules.forEach {
            val moduleJson = JsonObject()
            moduleJson.addProperty("state", it.state)
            it.values.also { values ->
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

    companion object {
        val DEFAULT_GSON = GsonBuilder().setPrettyPrinting().create()
    }
}