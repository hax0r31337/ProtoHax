package dev.sora.relay.game.registry

import org.cloudburstmc.nbt.NBTInputStream
import org.cloudburstmc.nbt.NbtList
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.protocol.common.DefinitionRegistry
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

class BlockMapping(private val runtimeToGameMap: Map<Int, BlockDefinition>, val airId: Int)
	: DefinitionRegistry<org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition> {

    private val gameToRuntimeMap = mutableMapOf<BlockDefinition, Int>()

    init {
        runtimeToGameMap.forEach { (k, v) ->
            gameToRuntimeMap[v] = k
        }
    }

    override fun getDefinition(runtimeId: Int): BlockDefinition {
        return runtimeToGameMap[runtimeId] ?: return UnknownBlockDefinition(runtimeId)
    }

    override fun isRegistered(definition: org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition): Boolean {
        return definition is UnknownBlockDefinition || getDefinition(definition.runtimeId) == definition
    }

    fun getRuntimeByIdentifier(identifier: String): Int {
        return gameToRuntimeMap.keys.find { it.identifier == identifier }?.runtimeId ?: 0
    }

	fun getRuntimeByDefinition(definition: BlockDefinition): Int {
		return gameToRuntimeMap[definition] ?: 0.also { println("no block found $definition") }
	}

    object Provider : MappingProvider<BlockMapping>() {

        override val resourcePath: String
            get() = "/assets/mcpedata/blocks"

		@Suppress("unchecked_cast")
        override fun readMapping(version: Short): BlockMapping {
            if (!availableVersions.contains(version)) error("Version not available: $version")

            val tag = NBTInputStream(DataInputStream(
                GZIPInputStream(MappingProvider::class.java.getResourceAsStream("${resourcePath}/runtime_block_states_$version.dat"))
            )).readTag() as NbtList<NbtMap>
            val runtimeToBlock = mutableMapOf<Int, BlockDefinition>()
            var airId = 0

            tag.forEach { subtag ->
                val runtime = subtag.getInt("runtimeId")
				val name = subtag.getString("name")
                if (name == "minecraft:air") {
                    airId = runtime
                }

                runtimeToBlock[runtime] = BlockDefinition(runtime, name, subtag.getCompound("states") ?: NbtMap.EMPTY)
            }

            return BlockMapping(runtimeToBlock, airId)
        }
    }
}
