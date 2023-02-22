package dev.sora.relay.game.registry

import com.google.gson.JsonParser
import org.cloudburstmc.protocol.common.DefinitionRegistry

class ItemMapping(private val runtimeToGameMap: Map<Int, String>) : DefinitionRegistry<org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition> {

    override fun getDefinition(runtimeId: Int): org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition {
        return ItemDefinition(runtimeId, runtimeToGameMap[runtimeId] ?: return UnknownItemDefinition(runtimeId))
    }

    override fun isRegistered(definition: org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition): Boolean {
        return definition is UnknownItemDefinition || getDefinition(definition.runtimeId) == definition
    }

    object Provider : MappingProvider<ItemMapping>() {

        override val resourcePath: String
            get() = "/assets/mcpedata/items"

        override fun readMapping(version: Short): ItemMapping {
            if (!availableVersions.contains(version)) error("Version not available: $version")

            val mapping = JsonParser
                .parseReader(MappingProvider::class.java.getResourceAsStream("$resourcePath/runtime_item_states_$version.json").reader(Charsets.UTF_8))
                .asJsonArray.associate {
                    val obj = it.asJsonObject
                    obj.get("id").asInt to obj.get("name").asString
                }

            return ItemMapping(mapping)
        }
    }
}