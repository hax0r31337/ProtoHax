package dev.sora.relay.game.registry

import com.google.gson.JsonParser
import dev.sora.relay.game.utils.constants.ItemTags
import org.cloudburstmc.protocol.common.DefinitionRegistry

class ItemMapping(private val runtimeToGameMap: MutableMap<Int, ItemDefinition>)
	: DefinitionRegistry<org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition> {

	private val gameToRuntimeMap = mutableMapOf<ItemDefinition, Int>()

	init {
		runtimeToGameMap.forEach { (k, v) ->
			gameToRuntimeMap[v] = k
		}
	}


	fun getRuntime(identifier: String): Int {
		return gameToRuntimeMap.keys.find { it.identifier == identifier }?.runtimeId ?: 0
	}

    override fun getDefinition(runtimeId: Int): org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition {
        return runtimeToGameMap[runtimeId] ?: return UnknownItemDefinition(runtimeId)
    }

    override fun isRegistered(definition: org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition): Boolean {
        return definition is UnknownItemDefinition || getDefinition(definition.runtimeId) == definition
    }

	fun registerCustomItems(itemDefinitions: List<org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition>) {
		itemDefinitions.forEach { itemDefinition ->
			val definition = ItemDefinition(itemDefinition.runtimeId, itemDefinition.identifier, ItemTags.tags(itemDefinition.identifier).toTypedArray())
			runtimeToGameMap[itemDefinition.runtimeId] = definition
			gameToRuntimeMap[definition] = itemDefinition.runtimeId
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

		override fun emptyMapping(): ItemMapping {
			return ItemMapping(mutableMapOf())
		}
    }
}
