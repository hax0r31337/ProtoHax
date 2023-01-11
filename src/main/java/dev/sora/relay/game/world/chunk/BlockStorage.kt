package dev.sora.relay.game.world.chunk

import com.nukkitx.nbt.NBTInputStream
import com.nukkitx.nbt.NbtMap
import com.nukkitx.nbt.util.stream.NetworkDataInputStream
import com.nukkitx.network.VarInts
import dev.sora.relay.game.utils.mapping.BlockMappingUtils
import dev.sora.relay.game.utils.mapping.RuntimeMapping
import dev.sora.relay.game.world.chunk.palette.BitArray
import dev.sora.relay.game.world.chunk.palette.BitArrayVersion
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import it.unimi.dsi.fastutil.ints.IntArrayList


class BlockStorage {

    var bitArray: BitArray
    var palette: IntArrayList

    constructor(blockMapping: RuntimeMapping, version: BitArrayVersion = BitArrayVersion.V2) {
        bitArray = version.createPalette(SIZE)
        palette = IntArrayList(16)
        palette.add(blockMapping.runtime("minecraft:air"))
    }

    constructor(byteBuf: ByteBuf, blockMapping: RuntimeMapping) {
        val paletteHeader = byteBuf.readByte().toInt()
        val isRuntime = (paletteHeader and 1) == 1
        val paletteVersion = paletteHeader or 1 shr 1

        val bitArrayVersion = BitArrayVersion.get(paletteVersion, true)

        val maxBlocksInSection = 4096 // 16*16*16

        bitArray = bitArrayVersion.createPalette(maxBlocksInSection)
        val wordsSize = bitArrayVersion.getWordsForSize(maxBlocksInSection)

        for (wordIterationIndex in 0 until wordsSize) {
            val word = byteBuf.readIntLE()
            bitArray.words[wordIterationIndex] = word
        }

        val paletteSize = VarInts.readInt(byteBuf)
        palette = IntArrayList(paletteSize)
        val nbtStream = if (isRuntime) null else NBTInputStream(NetworkDataInputStream(ByteBufInputStream(byteBuf)))
        for (i in 0 until paletteSize) {
            if (isRuntime) {
                palette.add(VarInts.readInt(byteBuf))
            } else {
                val map = (nbtStream!!.readTag() as NbtMap).toBuilder()
                val name = map["name"].toString()
                map.replace("name", if(!name.startsWith("minecraft:")) {
                    // For some reason, persistent chunks don't include the "minecraft:" that should be used in state names.
                    "minecraft:$name"
                } else {
                    name
                })
                palette.add(blockMapping.runtime(BlockMappingUtils.getBlockNameFromNbt(map.build())))
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
        val newBitArray = version.createPalette(SIZE)
        for (i in 0 until SIZE) {
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

    companion object {
        const val SIZE = 4096
    }
}