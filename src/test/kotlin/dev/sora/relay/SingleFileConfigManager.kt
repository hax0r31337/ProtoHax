package dev.sora.relay

import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.cheat.module.ModuleManager
import java.io.File
import java.io.Reader

class SingleFileConfigManager(moduleManager: ModuleManager) : AbstractConfigManager(moduleManager) {

    private val file = File("config.json")

    override fun listConfig(): List<String> {
        return listOf("default")
    }

    override fun loadConfigData(name: String): Reader {
        return file.reader(Charsets.UTF_8)
    }

    override fun saveConfigData(name: String, data: ByteArray) {
        file.writeBytes(data)
    }
}