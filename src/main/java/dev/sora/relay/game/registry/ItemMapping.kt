package dev.sora.relay.game.registry

import com.google.gson.JsonParser
import dev.sora.relay.game.utils.constants.ItemTags
import dev.sora.relay.utils.logInfo
import org.cloudburstmc.protocol.common.DefinitionRegistry

class ItemMapping(private val runtimeToGameMap: MutableMap<Int, ItemDefinition>)
	: DefinitionRegistry<org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition> {

    override fun getDefinition(runtimeId: Int): org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition {
        return runtimeToGameMap[runtimeId] ?: return UnknownItemDefinition(runtimeId)
    }

    override fun isRegistered(definition: org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition): Boolean {
        return definition is UnknownItemDefinition || getDefinition(definition.runtimeId) == definition
    }

	fun registerCustomItems(itemDefinitions: List<org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition>) {
		itemDefinitions.forEach { itemDefinition ->
			if (runtimeToGameMap.containsKey(itemDefinition.runtimeId)) {
				return@forEach
			}
			runtimeToGameMap[itemDefinition.runtimeId] = ItemDefinition(itemDefinition.runtimeId, itemDefinition.identifier, emptyArray())
			logInfo(itemDefinition.identifier)
		}
	}

    object Provider : MappingProvider<ItemMapping>() {

        override val resourcePath: String
            get() = "/assets/mcpedata/items"

        override fun readMapping(version: Short): ItemMapping {
            if (!availableVersions.contains(version)) error("Version not available: $version")

			val mapping = mutableMapOf<Int, ItemDefinition>()

			JsonParser
				.parseReader(MappingProvider::class.java.getResourceAsStream("$resourcePath/runtime_item_states_$version.json").reader(Charsets.UTF_8))
				.asJsonArray
				.forEach {
					val obj = it.asJsonObject
					val id = obj.get("id").asInt
					val name = obj.get("name").asString
					mapping[id] = ItemDefinition(id, name, ItemTags.tags(name).toTypedArray())
				}

            return ItemMapping(mapping)
        }
    }
}
