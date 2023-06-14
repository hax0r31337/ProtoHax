package dev.sora.relay

import dev.sora.relay.cheat.config.AbstractConfigManager
import java.io.File
import java.io.InputStream

class SingleFileConfigManager : AbstractConfigManager() {

    private val file = File("config.json")

    override fun listConfig(): List<String> {
        return listOf("default")
    }

    override fun loadConfigData(name: String): InputStream {
        return file.inputStream()
    }

    override fun saveConfigData(name: String, data: ByteArray) {
        file.writeBytes(data)
    }

    override fun deleteConfig(name: String): Boolean {
        file.delete()
        return false
    }

    override fun copyConfig(src: String, dst: String): Boolean {
        return false
    }

    override fun renameConfig(src: String, dst: String): Boolean {
        return false
    }
}
