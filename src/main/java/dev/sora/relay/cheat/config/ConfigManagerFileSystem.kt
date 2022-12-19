package dev.sora.relay.cheat.config

import dev.sora.relay.cheat.module.ModuleManager
import java.io.File
import java.io.Reader

class ConfigManagerFileSystem(private val dir: File, private val suffix: String, moduleManager: ModuleManager) : AbstractConfigManager(moduleManager) {

    init {
        if (!dir.exists())
            dir.mkdirs()
    }

    override fun listConfig(): List<String> {
        return (dir.listFiles() ?: return emptyList())
            .filter { it.name.endsWith(suffix) }
            .map { it.name.let { it.substring(0, it.length - suffix.length) } }
    }

    override fun loadConfigData(name: String): Reader? {
        val configFile = File(dir, "$name.json")
        if (!configFile.exists()) {
            return null
        }
        return configFile.reader(Charsets.UTF_8)
    }

    override fun saveConfigData(name: String, data: ByteArray) {
        val configFile = File(dir, "$name.json")
        configFile.writeBytes(data)
    }

    override fun deleteConfig(name: String) {
        val configFile = File(dir, "$name.json")
        if (configFile.exists())
            configFile.delete()
    }
}