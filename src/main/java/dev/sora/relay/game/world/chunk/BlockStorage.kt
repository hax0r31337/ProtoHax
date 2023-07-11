package dev.sora.relay.game.world.chunk

import dev.sora.relay.game.registry.BlockDefinition
import dev.sora.relay.game.registry.BlockMapping
import dev.sora.relay.game.world.chunk.palette.BitArray
import dev.sora.relay.game.world.chunk.palette.BitArrayVersion
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.cloudburstmc.nbt.NBTInputStream
import org.cloudburstmc.nbt.NBTOutputStream
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.nbt.util.stream.LittleEndianDataInputStream
import org.cloudburstmc.nbt.util.stream.LittleEndianDataOutputStream
import org.cloudburstmc.nbt.util.stream.NetworkDataInputStream
import org.cloudburstmc.nbt.util.stream.NetworkDataOutputStream
import org.cloudburstmc.protocol.common.util.VarInts


class BlockStorage {

    var bitArray: BitArray
    var palette: IntArrayList

    constructor(airId: Int, version: BitArrayVersion = BitArrayVersion.V2) {
        bitArray = version.createPalette(MAX_BLOCK_IN_SECTION)
        palette = IntArrayList(16)
        palette.add(airId)
    }

    constructor(buf: ByteBuf, blockMapping: BlockMapping, network: Boolean) {
        val paletteHeader = buf.readByte().toInt()
        val isRuntime = (paletteHeader and 1) == 1
        val paletteVersion = paletteHeader or 1 shr 1
        val bitArrayVersion = BitArrayVersion.get(paletteVersion, true)

        bitArray = bitArrayVersion.createPalette(MAX_BLOCK_IN_SECTION)

        for (i in bitArray.words.indices) {
            val word = buf.readIntLE()
            bitArray.words[i] = word
        }

		fun readInt(): Int {
			return if (network) {
				VarInts.readInt(buf)
			} else {
				buf.readIntLE()
			}
		}
        val paletteSize = readInt()
        palette = IntArrayList(paletteSize)
        val nbtStream = if (isRuntime) null else {
			val bis = ByteBufInputStream(buf)
			NBTInputStream(if (network) NetworkDataInputStream(bis) else LittleEndianDataInputStream(bis))
		}
        for (i in 0 until paletteSize) {
            if (isRuntime) {
                palette.add(readInt())
            } else {
                val map = nbtStream!!.readTag() as NbtMap
                palette.add(blockMapping.getRuntimeByDefinition(BlockDefinition(0, map.getString("name"), map.getCompound("states") ?: NbtMap.EMPTY)))
            }
        }
    }

    private fun getPaletteHeader(version: BitArrayVersion, runtime: Boolean): Int {
        return version.id.toInt() shl 1 or if (runtime) 1 else 0
    }

    private fun getIndex(x: Int, y: Int, z: Int): Int {
        return x shl 8 or (z shl 4) or y
    }

    fun setBlock(x: Int, y: Int, z: Int, runtimeId: Int) {
        this.setBlock(getIndex(x, y, z), runtimeId)
    }

    fun getByIndex(index: Int): Int {
        return palette.getInt(bitArray[index])
    }

    fun getBlock(x: Int, y: Int, z: Int): Int {
        return getByIndex(getIndex(x, y, z))
    }

    fun setBlock(index: Int, runtimeId: Int) {
        try {
            val id = idFor(runtimeId)
            bitArray[index] = id
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Unable to set block runtime ID: $runtimeId, palette: $palette", e)
        }
    }

    private fun onResize(version: BitArrayVersion) {
        val newBitArray = version.createPalette(MAX_BLOCK_IN_SECTION)
        for (i in 0 until MAX_BLOCK_IN_SECTION) {
            newBitArray[i] = bitArray[i]
        }
        bitArray = newBitArray
    }

    private fun idFor(runtimeId: Int): Int {
        var index = palette.indexOf(runtimeId)
        if (index != -1) {
            return index
        }
        index = palette.size
        val version = bitArray.version
        if (index > version.maxEntryValue) {
            val next = version.next()
            if (next != null) {
                onResize(next)
            }
        }
        palette.add(runtimeId)
        return index
    }

	/**
	 * use persistent nbt tags if [blockMapping] is pass, otherwise runtime id is used
	 */
	fun write(buf: ByteBuf, blockMapping: BlockMapping? = null, network: Boolean) {
		val bitArrayVersion = bitArray.version

		// palette header
		buf.writeByte(getPaletteHeader(bitArrayVersion, blockMapping == null))

		bitArray.words.forEach {
			buf.writeIntLE(it)
		}

		fun writeInt(int: Int) {
			if (network) {
				VarInts.writeInt(buf, int)
			} else {
				buf.writeIntLE(int)
			}
		}
		writeInt(palette.size)
		if (blockMapping == null) {
			palette.forEach {
				writeInt(it)
			}
		} else {
			val bos = ByteBufOutputStream(buf)
			val nbtos = NBTOutputStream(if (network) NetworkDataOutputStream(bos) else LittleEndianDataOutputStream(bos))
			palette.forEach {
                val def = blockMapping.getDefinition(it)
				val tag = NbtMap.builder()
                tag.putString("name", def.identifier)
				tag.putCompound("states", def.states)
				nbtos.writeTag(tag.build())
			}
		}
	}

    companion object {
        const val MAX_BLOCK_IN_SECTION = 4096
    }
}
