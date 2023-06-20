package dev.sora.relay.game.world

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*
import dev.sora.relay.game.registry.BlockDefinition
import dev.sora.relay.game.utils.constants.Dimension
import dev.sora.relay.game.world.chunk.Chunk
import dev.sora.relay.utils.logError
import kotlinx.coroutines.launch
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.SubChunkRequestResult
import org.cloudburstmc.protocol.bedrock.packet.*
import kotlin.math.floor

abstract class ChunkStorage(protected val session: GameSession, override val eventManager: EventManager) : Listenable {

	val chunks = mutableMapOf<Long, Chunk>()

	var dimension = Dimension.OVERWORLD
		protected set

	var viewDistance = -1
		protected set

	/**
	 * is 384 new world format supported by the server engine
	 */
	var is384WorldSupported = false
		private set

	private fun cleanUp() {
		chunks.forEach { (_, chunk) ->
			session.eventManager.emit(EventChunkUnload(session, chunk))
		}
		chunks.clear()
	}

	private val handleDisconnect = handle<EventDisconnect> {
		cleanUp()
	}

	private val handlePacketInbound = handle<EventPacketInbound> {
		if (packet is StartGamePacket) {
			is384WorldSupported = try {
				// 384 height world was introduced in minecraft 1.18
				val vanillaVersion = packet.vanillaVersion.split(".")
				vanillaVersion.size >= 2 && vanillaVersion[0] == "1" && vanillaVersion[1].toInt() >= 18
			} catch (e: Exception) {
				true
			}
		} else if (packet is LevelChunkPacket) {
			chunkOutOfRangeCheck()
			val chunk = Chunk(packet.chunkX, packet.chunkZ, dimension,
				is384WorldSupported && dimension == Dimension.OVERWORLD && (!session.netSessionInitialized || session.netSession.codec.protocolVersion >= 475),
				session.blockMapping)
			if (!packet.isCachingEnabled && !packet.isRequestSubChunks) {
				val buf = packet.data.retainedDuplicate()
				session.scope.launch {
					try {
						chunk.read(buf, packet.subChunksLength)
					} catch (t: Throwable) {
						logError("exception thrown whilst read chunk", t)
					} finally {
					    buf.release()
					}
				}
			} else if (packet.isCachingEnabled && !packet.isRequestSubChunks) {
				packet.blobIds.forEachIndexed { index, blobId ->
					if (index >= packet.subChunksLength) return@forEachIndexed
					session.cacheManager.registerCacheCallback(blobId) {
						chunk.readSubChunk(index, it)
					}
				}
			} // we handle SubChunkPackets for isCachingEnabled && isRequestSubChunks cases
			chunks[chunk.hash] = chunk
			session.eventManager.emit(EventChunkLoad(session, chunk))
		} else if (packet is ChunkRadiusUpdatedPacket) {
			viewDistance = packet.radius
			chunkOutOfRangeCheck()
		} else if (packet is ChangeDimensionPacket) {
			cleanUp()
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
						chunk.readSubChunk(position.y, it)
					}
				} else {
					val buf = it.data.retainedDuplicate()
					session.scope.launch {
						chunk.readSubChunk(position.y, buf)
					}
				}
			}
		}
	}

	protected fun chunkOutOfRangeCheck() {
		if (viewDistance <= 0) return
		val playerChunkX = floor(session.player.posX).toInt() shr 4
		val playerChunkZ = floor(session.player.posZ).toInt() shr 4
		val time = System.currentTimeMillis()
		chunks.entries.removeIf { (_, chunk) ->
			val bl = time - 10000 > chunk.loadedAt && !chunk.isInRadius(playerChunkX, playerChunkZ, viewDistance+1)
			if (bl) {
				val event = EventChunkUnload(session, chunk)
				session.eventManager.emit(event)
			}
			bl
		}
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

//	fun setBlockAt(x: Int, y: Int, z: Int, name: String) {
//		setBlockIdAt(x, y, z, session.blockMapping.getRuntimeByIdentifier(name))
//	}

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
