package dev.sora.relay.game.registry

import com.google.gson.JsonParser
import dev.sora.relay.game.utils.constants.ItemTags
import org.cloudburstmc.protocol.common.DefinitionRegistry

class ItemMapping(private val runtimeToGameMap: Map<Int, ItemDefinition>)
	: DefinitionRegistry<org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition> {

    override fun getDefinition(runtimeId: Int): org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition {
        return runtimeToGameMap[runtimeId] ?: return UnknownItemDefinition(runtimeId)
    }

    override fun isRegistered(definition: org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition): Boolean {
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
					val id = obj.get("id").asInt
					val name = obj.get("name").asString
                    id to ItemDefinition(id, name, ItemTags.tags(name).toTypedArray())
                }

            return ItemMapping(mapping)
        }
    }
}
