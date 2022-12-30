package dev.sora.relay.game.utils.mapping

import com.google.gson.JsonParser

abstract class AbstractMappingUtils {

    protected abstract val resourcePath: String

    protected open val emptyMapping = EmptyRuntimeMapping()
    protected open val availableVersions = JsonParser
        .parseReader(AbstractMappingUtils::class.java.getResourceAsStream("$resourcePath/index.json").reader(Charsets.UTF_8))
        .asJsonArray.map { it.asShort }.sortedBy { it }.toTypedArray()

    open fun craftItemMapping(protocolVersion: Int): RuntimeMapping {
        var version: Short = -1

        for (i in availableVersions) {
            if (i <= protocolVersion && version > i) {
                version = i
            }
        }

        return readMapping(version)
    }

    abstract fun readMapping(version: Short): RuntimeMapping
}