package dev.sora.relay.game.registry

import org.cloudburstmc.nbt.NBTInputStream
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.nbt.util.stream.NetworkDataInputStream
import java.util.zip.GZIPInputStream

object LegacyBlockMapping {

	private val stateToBlockMap: Map<Int, BlockDefinition>

	init {
		val inputStream = GZIPInputStream(MappingProvider::class.java.getResourceAsStream("/assets/mcpedata/legacy_block_states.nbt.gz"))
		val nbtStream = NBTInputStream(NetworkDataInputStream(inputStream))
		val stateToBlock = mutableMapOf<Int, BlockDefinition>()

		while (inputStream.available() > 0) {
			val tag = nbtStream.readTag() as NbtMap
			val name = tag.getString("name")
			val id = tag.getShort("id")
			val data = tag.getShort("data")

			stateToBlock[id.toInt() shl 6 or data.toInt()] = BlockDefinition(0, name, tag.getCompound("states") ?: NbtMap.EMPTY)
		}

		stateToBlockMap = stateToBlock
	}

    fun toBlockState(id: Int, data: Int)
        = toBlockState(id shl 6 or data)

	fun toBlockState(state: Int): BlockDefinition {
		return stateToBlockMap[state] ?: BlockDefinition(0, "minecraft:air", NbtMap.EMPTY)
	}

	fun BlockMapping.toRuntime(id: Int, data: Int): Int {
		return getRuntimeByDefinition(toBlockState(id, data))
	}

	fun BlockMapping.toRuntime(state: Int): Int {
		return getRuntimeByDefinition(toBlockState(state))
	}
}
