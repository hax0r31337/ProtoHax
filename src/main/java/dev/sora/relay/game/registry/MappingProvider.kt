package dev.sora.relay.game.registry

import com.google.gson.JsonParser

abstract class MappingProvider<T> {

    protected abstract val resourcePath: String

    protected open val availableVersions = JsonParser
        .parseReader(MappingProvider::class.java.getResourceAsStream("$resourcePath/index.json").reader(Charsets.UTF_8))
        .asJsonArray.map { it.asShort }.sortedBy { it }.toTypedArray()

    open fun craftMapping(protocolVersion: Int): T {
        return availableVersions.filter { it <= protocolVersion }.maxOrNull()?.let { readMapping(it) } ?: emptyMapping()
    }

    abstract fun readMapping(version: Short): T

	abstract fun emptyMapping(): T
}
