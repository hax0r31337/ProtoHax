package dev.sora.relay.game.utils.mapping

import com.google.gson.JsonParser

object ItemMappingUtils : AbstractMappingUtils() {

    override val resourcePath: String
        get() = "/assets/mcpedata/items"

    override fun readMapping(version: Short): RuntimeMapping {
        if (!availableVersions.contains(version)) return emptyMapping

        val mapping = JsonParser
            .parseReader(AbstractMappingUtils::class.java.getResourceAsStream("$resourcePath/runtime_item_states_$version.json").reader(Charsets.UTF_8))
            .asJsonArray.map {
                val obj = it.asJsonObject
                obj.get("name").asString to obj.get("id").asInt
            }
        return RuntimeMappingImpl(mapping)
    }
}