package dev.sora.relay.game.world

import com.nukkitx.math.vector.Vector3i
import com.nukkitx.protocol.bedrock.data.SubChunkRequestResult
import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.EventDisconnect
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.Listener
import dev.sora.relay.game.utils.constants.Dimension
import dev.sora.relay.game.world.chunk.Chunk
import io.netty.buffer.Unpooled

abstract class WorldwideBlockStorage(protected val session: GameSession) : Listener {

    protected val chunks = mutableMapOf<Long, Chunk>()

    var dimension = Dimension.OVERWORLD
        protected set

    var viewDistance = -1
        protected set

    open fun onDisconnect(event: EventDisconnect) {
        chunks.clear()
    }

    open fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet
        if (packet is LevelChunkPacket) {
            chunkOutOfRangeCheck()
            val chunk = Chunk(packet.chunkX, packet.chunkZ,
                dimension == Dimension.OVERWORLD && (!session.netSessionInitialized || session.netSession.packetCodec.protocolVersion >= 440),
                session.blockMapping, session.legacyBlockMapping)
            chunk.read(Unpooled.wrappedBuffer(packet.data), packet.subChunksLength)
            chunks[chunk.hash] = chunk
        } else if (packet is ChunkRadiusUpdatedPacket) {
            viewDistance = packet.radius
            chunkOutOfRangeCheck()
        } else if (packet is ChangeDimensionPacket) {
            chunks.clear()
        } else if (packet is UpdateBlockPacket && packet.dataLayer == 0) {
            setBlockIdAt(packet.blockPosition.x, packet.blockPosition.y, packet.blockPosition.z, packet.runtimeId)
        } else if (packet is SubChunkPacket && packet.dimension == dimension) {
            val centerPos = packet.centerPosition
            packet.subChunks.forEach {
                if (it.result != SubChunkRequestResult.SUCCESS) return@forEach
                val position = it.position.add(centerPos).add(0, 4, 0)
                val chunk = getChunk(position.x, position.z) ?: return@forEach
                if (it.data.isEmpty()) {
                    // cached chunk
                    session.cacheManager.registerCacheCallback(it.blobId) {
                        chunk.readSubChunk(position.y, it)
                    }
                } else {
                    chunk.readSubChunk(position.y, it.data)
                }
            }
        }
    }

    private fun chunkOutOfRangeCheck() {
        // if (viewDistance < 0) return
        // val playerChunkX = floor(session.thePlayer.posX).toInt() shr 4
        // val playerChunkZ = floor(session.thePlayer.posZ).toInt() shr 4
        // chunks.entries.removeIf { (_, chunk) ->
        //     !chunk.isInRadius(playerChunkX, playerChunkZ, viewDistance+1)
        // }
    }

    fun getBlockIdAt(x: Int, y: Int, z: Int): Int {
        val chunk = getChunkAt(x, z) ?: return session.blockMapping.runtime("minecraft:air")
        return chunk.getBlockAt(x and 0x0f, y, z and 0x0f)
    }

    fun getBlockAt(x: Int, y: Int, z: Int): String {
        return session.blockMapping.game(getBlockIdAt(x, y, z))
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
        setBlockIdAt(x, y, z, session.blockMapping.runtime(name))
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

    override fun listen() = true
}