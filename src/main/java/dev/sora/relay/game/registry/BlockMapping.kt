package dev.sora.relay.game.registry

import org.cloudburstmc.nbt.NBTInputStream
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.nbt.util.stream.NetworkDataInputStream
import org.cloudburstmc.protocol.bedrock.data.BlockPropertyData
import org.cloudburstmc.protocol.common.DefinitionRegistry
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

class BlockMapping(private val runtimeToGameMap: MutableMap<Int, BlockDefinition>, var airId: Int)
	: DefinitionRegistry<org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition> {

    private val gameToRuntimeMap = mutableMapOf<BlockDefinition, Int>()

	private var hasRegisteredCustomBlocks = false

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
		return gameToRuntimeMap[definition] ?: 0
	}

	fun registerCustomBlocksFNV(customBlocks: List<BlockPropertyData>) {
		if (hasRegisteredCustomBlocks) {
			throw IllegalStateException("Custom blocks has already registered once!")
		}
		hasRegisteredCustomBlocks = true

		// custom blocks will cause runtime id to shift
		val blockDefinitionList = mutableListOf<BlockDefinition>()
		blockDefinitionList.addAll(runtimeToGameMap.values)

		customBlocks.forEach { blockPropertyData ->
			blockDefinitionList.add(BlockDefinition(0, blockPropertyData.name, NbtMap.EMPTY))
		}

		runtimeToGameMap.clear()
		gameToRuntimeMap.clear()

		blockDefinitionList
			.sortedWith(HashedPaletteComparator())
			.forEachIndexed { index, blockDefinition ->
				blockDefinition.runtimeId = index
				runtimeToGameMap[index] = blockDefinition
				gameToRuntimeMap[blockDefinition] = index

				if (blockDefinition.identifier == Provider.AIR_IDENTIFIER) {
					airId = index
				}
			}
	}

    object Provider : MappingProvider<BlockMapping>() {

		const val AIR_IDENTIFIER = "minecraft:air"

        override val resourcePath: String
            get() = "/assets/mcpedata/blocks"

		override fun readMapping(version: Short): BlockMapping {
            if (!availableVersions.contains(version)) error("Version not available: $version")

			val inputStream = GZIPInputStream(MappingProvider::class.java.getResourceAsStream("${resourcePath}/canonical_block_states_$version.nbt.gz"))
            val nbtStream = NBTInputStream(NetworkDataInputStream(inputStream))
            val runtimeToBlock = mutableMapOf<Int, BlockDefinition>()
            var airId = 0
			var runtime = 0

            while (inputStream.available() > 0) {
				val tag = nbtStream.readTag() as NbtMap
				val name = tag.getString("name")
				if (name == AIR_IDENTIFIER) {
					airId = runtime
				}

				runtimeToBlock[runtime] = BlockDefinition(runtime, name, tag.getCompound("states") ?: NbtMap.EMPTY)

				runtime++
			}

            return BlockMapping(runtimeToBlock, airId)
        }

		override fun emptyMapping(): BlockMapping {
			return BlockMapping(mutableMapOf(), 0)
		}
    }

	/**
	 * the block palette order algorithm introduced in Minecraft 1.18.30
	 * https://gist.github.com/SupremeMortal/5e09c8b0eb6b3a30439b317b875bc29c
	 *
	 * @author SupremeMortal
	 */
	class HashedPaletteComparator : Comparator<BlockDefinition> {

		private val FNV1_64_INIT = -0x340d631b7bdddcdbL
		private val FNV1_PRIME_64 = 1099511628211L

		override fun compare(o1: BlockDefinition, o2: BlockDefinition): Int {
			return compare(o1.identifier, o2.identifier)
		}

		fun compare(o1: String, o2: String): Int {
			val bytes1 = o1.toByteArray(StandardCharsets.UTF_8)
			val bytes2 = o2.toByteArray(StandardCharsets.UTF_8)
			val hash1 = fnv164(bytes1)
			val hash2 = fnv164(bytes2)

			return java.lang.Long.compareUnsigned(hash1, hash2)
		}

		private fun fnv164(data: ByteArray): Long {
			var hash = FNV1_64_INIT
			for (datum in data) {
				hash *= FNV1_PRIME_64
				hash = hash xor (datum.toInt() and 0xff).toLong()
			}
			return hash
		}
	}
}
