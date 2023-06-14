package dev.sora.relay.cheat.config

import dev.sora.relay.utils.logError
import java.io.File
import java.io.InputStream

class ConfigManagerFileSystem(private val dir: File, private val suffix: String) : AbstractConfigManager() {

    init {
        if (!dir.exists())
            dir.mkdirs()
    }

    fun getConfigFile(name: String): File {
        return File(dir, "$name.json")
    }

    override fun listConfig(): List<String> {
        return (dir.listFiles() ?: return emptyList())
            .filter { it.name.endsWith(suffix) }
            .map { it.name.let { it.substring(0, it.length - suffix.length) } }
    }

    override fun loadConfigData(name: String): InputStream? {
        val configFile = getConfigFile(name)
        if (!configFile.exists()) {
            return null
        }
        return configFile.inputStream()
    }

    override fun saveConfigData(name: String, data: ByteArray) {
        val configFile = getConfigFile(name)
        configFile.writeBytes(data)
    }

    override fun deleteConfig(name: String): Boolean {
        val configFile = getConfigFile(name)
        if (configFile.exists())
            return configFile.delete()
        return false
    }

    override fun copyConfig(src: String, dst: String): Boolean {
        val srcFile = getConfigFile(src)
        if (!srcFile.exists()) return false
        val dstFile = getConfigFile(dst)
        if (dstFile.exists()) return false
        return try {
            srcFile.copyTo(dstFile)
            true
        } catch (t: Throwable) {
            logError("error whilst copy config", t)
            false
        }
    }

    override fun renameConfig(src: String, dst: String): Boolean {
        val srcFile = getConfigFile(src)
        if (!srcFile.exists()) return false
        val dstFile = getConfigFile(dst)
        if (dstFile.exists()) return false
        srcFile.renameTo(dstFile)
        return true
    }
}
