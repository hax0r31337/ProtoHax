package dev.sora.relay.game.world.chunk

import dev.sora.relay.game.utils.mapping.RuntimeMapping
import io.netty.buffer.ByteBuf

class ChunkSection(private val blockMapping: RuntimeMapping,
                   private val legacyBlockMapping: RuntimeMapping) {

    var storage = BlockStorage(blockMapping)
        private set

    /**
     * deserialize chunk into blocks
     * credit:
     * https://github.com/CloudburstMC/Nukkit/blob/b391d4ddd0a6db7eb1c830f5a5477f5a6d3ea459/src/main/java/cn/nukkit/level/format/generic/serializer/NetworkChunkSerializer.java
     * https://github.com/DavyCraft648/Barrel/blob/ebd52e4a7b7fa17e2d3f206690e4516088eff71c/src/main/java/org/barrelmc/barrel/network/translator/bedrock/LevelChunkPacket.java
     */
    fun read(buf: ByteBuf) {
        val version = buf.readByte().toInt()
        if (version == 0) {
            // PocketMine-MP still using this format
            readLegacy(buf)
        } else if (version == 1 || version in 8..10) {
            readModern(buf, version)
        } else {
            throw UnsupportedOperationException("chunk version not supported: $version")
        }
    }

    private fun readModern(buf: ByteBuf, version: Int) {
        val layers = if(version == 1) 1 else buf.readByte().toInt()
        if (version >= 9) {
            buf.readByte()
        }
        if (layers == 0) return
        storage = BlockStorage(buf, blockMapping)

        // consume other layers that we don't need
        repeat(layers - 1) {
            BlockStorage(buf, blockMapping)
        }
    }

    private fun readLegacy(buf: ByteBuf) {
        val blockIds = ByteArray(4096)
        buf.readBytes(blockIds)

        val metaIds = ByteArray(2048)
        buf.readBytes(metaIds)

        var index = 0
        for (x in 0..15) {
            for (z in 0..15) {
                for (y in 0..15) {
                    val idx = (x shl 8) + (z shl 4) + y
                    val id = blockIds[idx].toInt()
                    val meta = metaIds[idx shr 1].toInt() shr (idx and 1) * 4 and 15
                    val name = legacyBlockMapping.game(id shl 6 or meta)
                    storage.setBlock(index, blockMapping.runtime(name))
                    index++
                }
            }
        }
    }

    fun getBlockAt(x: Int, y: Int, z: Int): Int {
        assert(x in 0..15 && y in 0..15 && z in 0..15) { "query out of range (x=$x, y=$y, z=$z)" }
        return storage.getBlock(x, y, z)
    }

    fun setBlockAt(x: Int, y: Int, z: Int, runtimeId: Int) {
        assert(x in 0..15 && y in 0..15 && z in 0..15) { "query out of range (x=$x, y=$y, z=$z)" }
        storage.setBlock(x, y, z, runtimeId)
    }
}