package dev.sora.relay.cheat.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.config.section.IConfigSection
import dev.sora.relay.utils.logError
import java.io.InputStream

abstract class AbstractConfigManager {

	protected val sections = mutableListOf<IConfigSection>()

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

	fun addSection(section: IConfigSection) {
		sections.add(section)
	}

    /**
     * @return false if failed to load the config or config not exists
     */
    open fun loadConfig(name: String): Boolean {
        try {
            val json = JsonParser.parseReader((loadConfigData(name) ?: return false).reader(Charsets.UTF_8)).asJsonObject

			sections.forEach {
				it.load(json.get(it.sectionName))
			}

            return true
        } catch (t: Throwable) {
            logError("failed to load config", t)
            return false
        }
    }

    open fun saveConfig(name: String) {
        try {
            val json = JsonObject()

			sections.forEach {
				val element = it.save() ?: return@forEach
				json.add(it.sectionName, element)
			}

            saveConfigData(name, DEFAULT_GSON.toJson(json).toByteArray(Charsets.UTF_8))
        } catch (t: Throwable) {
            logError("failed to save config", t)
        }
    }

    companion object {
        val DEFAULT_GSON = GsonBuilder().setPrettyPrinting().create()
    }
}
