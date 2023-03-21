package dev.sora.relay.cheat.command.impl

import dev.sora.relay.cheat.command.Command
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*
import dev.sora.relay.game.world.chunk.Chunk
import dev.sora.relay.game.world.leveldb.LevelDBLevelData
import dev.sora.relay.game.world.leveldb.LevelDBWorld
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CommandDownloadWorld(override val eventManager: EventManager, private val saveFolder: File) : Command("wdl"), Listenable {

	override var handleEvents = false
	private val cachedChunks = mutableListOf<Pair<Chunk, Int>>()

	private val handleChunkLoad = handle<EventChunkLoad> { event ->
		val currentDimension = event.session.theWorld.dimension
		cachedChunks.removeIf { (chunk, dimension) ->
			chunk.x == event.chunk.x && chunk.z == event.chunk.z && currentDimension == dimension
		}
		cachedChunks.add(event.chunk to currentDimension)
	}

	private val handleDisconnect = handle<EventDisconnect> {
		saveWorld(it.session)
	}

	/**
	 * used to show status tooltip
	 */
	private val handleTick = handle<EventTick> {
		it.session.sendPacketToClient(TextPacket().apply {
			type = TextPacket.Type.TIP
			message = "§7[§bWorldDownloader§7]§f Download chunks: ${cachedChunks.size}"
			xuid = ""
		})
	}

	private fun saveWorld(session: GameSession) {
		handleEvents = false
		val folder = File(saveFolder, ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).also {
			it.mkdirs()
		}

		// save chunks
		val world = LevelDBWorld(File(folder, "db"))
		cachedChunks.forEach { (chunk, dimension) ->
			world.saveChunk(chunk, dimension)
		}
		world.close()

		// save level dat
		val levelData = LevelDBLevelData(session.netSession.codec.protocolVersion, session.netSession.codec.minecraftVersion)
		File(folder, "level.dat").writeBytes(levelData.toBytes())

		session.chat("Saved ${cachedChunks.size} chunks to ${folder.name}")
		cachedChunks.clear()
	}

	override fun exec(args: Array<String>, session: GameSession) {
		if (args.isEmpty()) {
			session.chat("-wdl <listen/save/cancel>")
			return
		}

		when(args[0].lowercase()) {
			"listen" -> {
				val currentDimension = session.theWorld.dimension
				session.theWorld.chunks.values.forEach {
					cachedChunks.add(it to currentDimension)
				}
				handleEvents = true
				session.chat("Start listening for chunks")
			}
			"save" -> {
				session.chat("Saving...")
				saveWorld(session)
			}
			"cancel" -> {
				handleEvents = false
				cachedChunks.clear()
				session.chat("Cancelled")
			}
			else -> session.chat("-wdl <listen/save/cancel>")
		}
	}
}
