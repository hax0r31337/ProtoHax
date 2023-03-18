package dev.sora.relay.game.registry

import com.google.gson.JsonParser

abstract class MappingProvider<T> {

    protected abstract val resourcePath: String

    protected open val availableVersions = JsonParser
        .parseReader(MappingProvider::class.java.getResourceAsStream("$resourcePath/index.json").reader(Charsets.UTF_8))
        .asJsonArray.map { it.asShort }.sortedBy { it }.toTypedArray()

    open fun craftMapping(protocolVersion: Int): T {
        var version: Short = -1

        for (i in availableVersions) {
            if (i in version..protocolVersion) {
                version = i
            }
        }

        return readMapping(version)
    }

    abstract fun readMapping(version: Short): T
}