package dev.sora.relay.game.world.chunk

import dev.sora.relay.game.utils.mapping.RuntimeMapping
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.math.abs

class Chunk(val x: Int, val z: Int, val is384World: Boolean,
            private val blockMapping: RuntimeMapping, private val legacyBlockMapping: RuntimeMapping) {

    val hash: Long
        get() = hash(x, z)

    private val sectionStorage = Array(if (is384World) 24 else 16) { ChunkSection(blockMapping, legacyBlockMapping) }
    private val maxiumHeight = sectionStorage.size * 16

    fun isInRadius(playerChunkX: Int, playerChunkZ: Int, radius: Int): Boolean {
        return abs(x - playerChunkX) <= radius && abs(z - playerChunkZ) <= radius
    }

    fun read(buf: ByteBuf, subChunks: Int) {
        repeat(subChunks) {
            readSubChunk(it, buf)
        }
    }

    fun readSubChunk(index: Int, data: ByteArray) {
        readSubChunk(index, Unpooled.wrappedBuffer(data))
    }

    fun readSubChunk(index: Int, buf: ByteBuf) {
        sectionStorage[index].read(buf)
    }

    fun getBlockAt(x: Int, yIn: Int, z: Int): Int {
        val y = if(is384World) yIn + 64 else yIn
        assert(y in 0..maxiumHeight)

        return sectionStorage[y shr 4].getBlockAt(x, y and 0x0f, z)
    }

    fun setBlockAt(x: Int, yIn: Int, z: Int, runtimeId: Int) {
        val y = if(is384World) yIn + 64 else yIn
        assert(y in 0..maxiumHeight)

        sectionStorage[y shr 4].setBlockAt(x, y and 0x0f, z, runtimeId)
    }

    companion object {
        fun hash(x: Int, z: Int): Long {
            return x.toLong() shl 32 or (z.toLong() and 0xffffffffL)
        }
    }
}