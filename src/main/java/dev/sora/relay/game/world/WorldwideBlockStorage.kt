package dev.sora.relay.game.world

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*
import dev.sora.relay.game.registry.BlockDefinition
import dev.sora.relay.game.utils.constants.Dimension
import dev.sora.relay.game.world.chunk.Chunk
import dev.sora.relay.utils.logError
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.SubChunkRequestResult
import org.cloudburstmc.protocol.bedrock.packet.*

abstract class WorldwideBlockStorage(protected val session: GameSession, override val eventManager: EventManager) : Listenable {

	val chunks = mutableMapOf<Long, Chunk>()

	var dimension = Dimension.OVERWORLD
		protected set

	var viewDistance = -1
		protected set

	private val handleDisconnect = handle<EventDisconnect> {
		chunks.clear()
	}

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet

		if (packet is SubChunkRequestPacket) {
			println(packet)
		}
	}

	private val handlePacketInbound = handle<EventPacketInbound> { event ->
		val packet = event.packet

		if (packet is LevelChunkPacket) {
			chunkOutOfRangeCheck()
			val chunk = Chunk(packet.chunkX, packet.chunkZ,
				dimension == Dimension.OVERWORLD && (!session.netSessionInitialized || session.netSession.codec.protocolVersion >= 440),
				session.blockMapping, session.legacyBlockMapping)
			if (!packet.isCachingEnabled && !packet.isRequestSubChunks) {
				val readerIndex = packet.data.readerIndex()
				try {
					chunk.read(packet.data, packet.subChunksLength)
				} catch (t: Throwable) {
					logError("exception thrown whilst read chunk", t)
				}
				packet.data.readerIndex(readerIndex)
			} else if (packet.isCachingEnabled && !packet.isRequestSubChunks) {
				packet.blobIds.forEachIndexed { index, blobId ->
					if (index >= packet.subChunksLength) return@forEachIndexed
					session.cacheManager.registerCacheCallback(blobId) {
						val readerIndex = it.readerIndex()
						try {
							chunk.readSubChunk(index, it)
						} catch (t: Throwable) {
							logError("exception thrown whilst read subchunk", t)
						}
						it.readerIndex(readerIndex)
					}
				}
			} // we handle SubChunkPackets for isCachingEnabled && isRequestSubChunks cases
			chunks[chunk.hash] = chunk
			session.eventManager.emit(EventChunkLoad(session, chunk))
		} else if (packet is ChunkRadiusUpdatedPacket) {
			viewDistance = packet.radius
			chunkOutOfRangeCheck()
		} else if (packet is ChangeDimensionPacket) {
			chunks.clear()
			session.eventManager.emit(EventDimensionChange(session, dimension))
		} else if (packet is UpdateBlockPacket && packet.dataLayer == 0) {
			setBlockIdAt(packet.blockPosition.x, packet.blockPosition.y, packet.blockPosition.z, packet.definition.runtimeId)
		} else if (packet is SubChunkPacket && packet.dimension == dimension) {
			val centerPos = packet.centerPosition
			packet.subChunks.forEach {
				if (it.result != SubChunkRequestResult.SUCCESS) return@forEach
				val position = it.position.add(centerPos).add(0, 4, 0)
				val chunk = getChunk(position.x, position.z) ?: return@forEach
				if (it.data.readableBytes() == 0) {
					// cached chunk
					session.cacheManager.registerCacheCallback(it.blobId) {
						val readerIndex = it.readerIndex()
						try {
							chunk.readSubChunk(position.y, it)
						} catch (t: Throwable) {
							logError("exception thrown whilst read subchunk", t)
						}
						it.readerIndex(readerIndex)
					}
				} else {
					val readerIndex = it.data.readerIndex()
					try {
						chunk.readSubChunk(position.y, it.data)
					} catch (t: Throwable) {
						logError("exception thrown whilst read subchunk", t)
					}
					it.data.readerIndex(readerIndex)
				}
			}
		}
	}

	protected fun chunkOutOfRangeCheck() {
		// TODO: fix

//         if (viewDistance < 0) return
//         val playerChunkX = floor(session.thePlayer.posX).toInt() shr 4
//         val playerChunkZ = floor(session.thePlayer.posZ).toInt() shr 4
//         chunks.entries.removeIf { (_, chunk) ->
//             !chunk.isInRadius(playerChunkX, playerChunkZ, viewDistance+1)
//         }
	}

	fun getBlockIdAt(x: Int, y: Int, z: Int): Int {
		val chunk = getChunkAt(x, z) ?: return session.blockMapping.airId
		return chunk.getBlockAt(x and 0x0f, y, z and 0x0f)
	}

	fun getBlockAt(x: Int, y: Int, z: Int): BlockDefinition {
		return session.netSession.peer.codecHelper.blockDefinitions.getDefinition(getBlockIdAt(x, y, z)) as BlockDefinition
	}

	fun getBlockAt(vec: Vector3i)
		= getBlockAt(vec.x, vec.y, vec.z)

	fun getBlockIdAt(vec: Vector3i)
		= getBlockIdAt(vec.x, vec.y, vec.z)

	fun setBlockIdAt(x: Int, y: Int, z: Int, id: Int) {
		val chunk = getChunkAt(x, z) ?: return
		chunk.setBlockAt(x and 0x0f, y, z and 0x0f, id)
	}

	fun setBlockAt(x: Int, y: Int, z: Int, name: String) {
		setBlockIdAt(x, y, z, session.blockMapping.getRuntimeByIdentifier(name))
	}

	fun setBlockAt(x: Int, y: Int, z: Int, block: BlockDefinition) {
		setBlockIdAt(x, y, z, block.runtimeId)
	}

	/**
	 * get chunk by chunk position
	 */
	fun getChunk(chunkX: Int, chunkZ: Int): Chunk? {
		return chunks[Chunk.hash(chunkX, chunkZ)]
	}

	/**
	 * get chunk by actual position
	 */
	fun getChunkAt(x: Int, z: Int): Chunk? {
		return getChunk(x shr 4, z shr 4)
	}
}
