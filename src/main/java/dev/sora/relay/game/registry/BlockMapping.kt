package dev.sora.relay.game.registry

import org.cloudburstmc.nbt.NBTInputStream
import org.cloudburstmc.nbt.NbtList
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.protocol.common.DefinitionRegistry
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

class BlockMapping(private val runtimeToGameMap: Map<Int, BlockDefinition>, val airId: Int) : DefinitionRegistry<org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition> {

    private val gameToRuntimeMap = mutableMapOf<String, Int>()

    init {
        runtimeToGameMap.forEach { (k, v) ->
            gameToRuntimeMap[v.identifier] = k
        }
    }

    override fun getDefinition(runtimeId: Int): org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition {
        return runtimeToGameMap[runtimeId] ?: return UnknownBlockDefinition(runtimeId)
    }

    override fun isRegistered(definition: org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition): Boolean {
        return definition is UnknownBlockDefinition || getDefinition(definition.runtimeId) == definition
    }

    fun getRuntimeByIdentifier(identifier: String): Int {
        return gameToRuntimeMap[identifier] ?: 0
    }

    object Provider : MappingProvider<BlockMapping>() {

        override val resourcePath: String
            get() = "/assets/mcpedata/blocks"

        override fun readMapping(version: Short): BlockMapping {
            if (!availableVersions.contains(version)) error("Version not available: $version")

            val tag = NBTInputStream(DataInputStream(
                GZIPInputStream(MappingProvider::class.java.getResourceAsStream("${resourcePath}/runtime_block_states_$version.dat"))
            )).readTag() as NbtList<NbtMap>
            val runtimeToBlock = mutableMapOf<Int, BlockDefinition>()
            var airId = 0

            tag.forEach { subtag ->
                val name = getBlockNameFromNbt(subtag)
                val runtime = subtag.getInt("runtimeId")

                if (name == "minecraft:air") {
                    airId = runtime
                }

                runtimeToBlock[runtime] = BlockDefinition(runtime, name, subtag)
            }

            return BlockMapping(runtimeToBlock, airId)
        }

        fun getBlockNameFromNbt(nbt: NbtMap): String {
            val sb = StringBuilder()
            sb.append(nbt.getString("name"))
            val stateMap = (nbt.getCompound("states") ?: NbtMap.builder().build())
                .map { it }.sortedBy { it.key }
            if(stateMap.isNotEmpty()) {
                sb.append("[")
                stateMap.forEach { (key, value) ->
                    sb.append(key)
                    sb.append("=")
                    sb.append(value)
                    sb.append(",")
                }
                sb.delete(sb.length - 1, sb.length)
                sb.append("]")
            }
            return sb.toString()
        }
    }
}
