package dev.sora.relay.game.world.leveldb

import dev.sora.relay.game.world.chunk.Chunk
import dev.sora.relay.game.world.chunk.ChunkSection
import io.netty.buffer.ByteBufAllocator
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File

/**
 * From PowerNukkitX
 * @author Superrice666, NekoRibbon
 */
class LevelDBWorld(val folder: File) {

	private val db = Iq80DBFactory.factory.open(folder, Options().createIfMissing(true).compressionType(CompressionType.ZLIB_RAW))

	fun close() {
		db.close()
	}

	fun saveChunkVersion(x: Int, z: Int, dimension: Int, version: Byte = CHUNK_VERSION) {
		val key = LevelDBChunkKey.VERSION.getKey(x, z, dimension)

		db.put(key, byteArrayOf(version))
	}

	fun saveSubChunk(x: Int, z: Int, dimension: Int, y: Int, subChunk: ChunkSection, useRuntime: Boolean) {
//		if (!subChunk.populated) {
//			return
//		}

		val key = LevelDBChunkKey.SUB_CHUNK_DATA.getKey(x, z, dimension, y)

		val buf = ByteBufAllocator.DEFAULT.ioBuffer()
		try {
			subChunk.write(buf, useRuntime, false)
			val data = ByteArray(buf.readableBytes())
			buf.readBytes(data)
			db.put(key, data)
		} finally {
		    buf.release()
		}
	}

	fun saveChunk(chunk: Chunk) {
		saveChunkVersion(chunk.x, chunk.z, chunk.dimension)

		val yOffset = if (chunk.is384World) -4 else 0
		chunk.sectionStorage.forEachIndexed { i, subChunk ->
			saveSubChunk(chunk.x, chunk.z, chunk.dimension, i + yOffset, subChunk, false)
		}
	}

	companion object {
		private const val CHUNK_VERSION = 0x28.toByte()
	}
}
