package dev.sora.relay.game.registry

import dev.sora.relay.game.utils.BlockHashFNV32
import org.cloudburstmc.nbt.NBTInputStream
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.nbt.util.stream.NetworkDataInputStream
import org.cloudburstmc.protocol.bedrock.data.BlockPropertyData
import org.cloudburstmc.protocol.common.DefinitionRegistry
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

class BlockMapping(val protocol: Short, private val runtimeToGameMap: MutableMap<Int, BlockDefinition>, var airId: Int)
	: DefinitionRegistry<org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition> {

    private val gameToRuntimeMap = mutableMapOf<BlockDefinition, Int>()

	private var runtimeIdHashed = false

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

	fun registerCustomBlocks(customBlocks: List<BlockPropertyData>, runtimeIdHashed: Boolean) {
		if (protocol >= 503) {
			registerCustomBlocksFNV(customBlocks, runtimeIdHashed)
		}
	}

	private fun registerCustomBlocksFNV(customBlocks: List<BlockPropertyData>, runtimeIdHashed: Boolean) {
		// custom blocks will cause runtime id to shift
		val blockDefinitionList = mutableListOf<BlockDefinition>()
		blockDefinitionList.addAll(runtimeToGameMap.values)

		var hasChanges = false
		customBlocks.forEach { blockPropertyData ->
			val definition = BlockDefinition(0, blockPropertyData.name, NbtMap.EMPTY)
			if (!blockDefinitionList.contains(definition)) {
				blockDefinitionList.add(definition)
				hasChanges = true
			}
		}
		if (!hasChanges && this.runtimeIdHashed == runtimeIdHashed) {
			// no changes has applied to block mapping
			return
		}

		this.runtimeIdHashed = runtimeIdHashed
		updateRuntimeId(blockDefinitionList)
	}

	private fun updateRuntimeId(blockDefinitionList: List<BlockDefinition>) {
		runtimeToGameMap.clear()
		gameToRuntimeMap.clear()

		if (runtimeIdHashed) {
			blockDefinitionList.forEach { blockDefinition ->
				val nbt = NbtMap.builder()
					.putString("name", blockDefinition.identifier)
					.putCompound("states", blockDefinition.states)
					.build()
				val runtimeId = BlockHashFNV32.createHash(nbt)

				blockDefinition.runtimeId = runtimeId
				runtimeToGameMap[runtimeId] = blockDefinition
				gameToRuntimeMap[blockDefinition] = runtimeId
			}
		} else {
			blockDefinitionList
				.sortedWith(HashedPaletteComparator)
				.forEachIndexed { index, blockDefinition ->
					blockDefinition.runtimeId = index
					runtimeToGameMap[index] = blockDefinition
					gameToRuntimeMap[blockDefinition] = index

					if (blockDefinition.identifier == Provider.AIR_IDENTIFIER) {
						airId = index
					}
				}
		}
	}

	fun setRuntimeIdHashed(hashed: Boolean) {
		if (runtimeIdHashed != hashed) return
		runtimeIdHashed = hashed

		updateRuntimeId(runtimeToGameMap.values.map { it })
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

            return BlockMapping(version, runtimeToBlock, airId)
        }

		override fun emptyMapping(): BlockMapping {
			return BlockMapping(0, mutableMapOf(), 0)
		}
    }

	/**
	 * the block palette order algorithm introduced in Minecraft 1.18.30
	 * https://gist.github.com/SupremeMortal/5e09c8b0eb6b3a30439b317b875bc29c
	 *
	 * @author SupremeMortal
	 */
	object HashedPaletteComparator : Comparator<BlockDefinition> {

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
