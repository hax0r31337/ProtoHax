package dev.sora.relay.cheat.command.impl

import dev.sora.relay.cheat.command.Command
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*
import dev.sora.relay.game.world.leveldb.LevelDBLevelData
import dev.sora.relay.game.world.leveldb.LevelDBWorld
import kotlinx.coroutines.launch
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CommandDownloadWorld(override val eventManager: EventManager, private val saveFolder: File) : Command("wdl"), Listenable {

	override var handleEvents = false

	private var level: LevelDBWorld? = null
	private var chunkSaved = 0

	private val handleChunkUnload = handle<EventChunkUnload> {
		level ?: return@handle
		level!!.saveChunk(chunk)
		chunkSaved++
	}

	private val handleDisconnect = handle<EventDisconnect> {
		saveWorld(session)
	}

	/**
	 * used to show status tooltip
	 */
	private val handleTick = handle<EventTick> {
		session.sendPacketToClient(TextPacket().apply {
			type = TextPacket.Type.TIP
			message = "§7[§bWorldDownloader§7]§f Download chunks: ${chunkSaved}"
			xuid = ""
		})
	}

	private fun startSaveWorld() {
		chunkSaved = 0

		val folder = File(saveFolder, ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).also {
			it.mkdirs()
		}
		level = LevelDBWorld(File(folder, "db"))
	}

	private fun saveWorld(session: GameSession) {
		handleEvents = false

		// save chunks
		level ?: return
		session.level.chunks.forEach { (_, chunk) ->
			level!!.saveChunk(chunk)
			chunkSaved++
		}
		level!!.close()

		// save level.dat
		val levelData = LevelDBLevelData(session.netSession.codec.protocolVersion, session.netSession.codec.minecraftVersion)
		val parent = level!!.folder.parentFile
		level = null
		File(parent, "level.dat").writeBytes(levelData.toBytes())

		session.chat("Saved $chunkSaved chunks to ${parent.canonicalPath}")
	}

	override fun exec(args: Array<String>, session: GameSession) {
		if (args.isEmpty()) {
			session.chat("-wdl <listen/save/cancel>")
			return
		}

		when(args[0].lowercase()) {
			"listen" -> {
				handleEvents = true
				session.chat("Start listening for chunks")
				startSaveWorld()
			}
			"save" -> {
				session.chat("Saving...")
				session.scope.launch {
					saveWorld(session)
				}
			}
			"cancel" -> {
				handleEvents = false
				level?.let {
					it.close()
					it.folder.parentFile.deleteRecursively()
					level = null
				}
				session.chat("Cancelled")
			}
			else -> session.chat("-wdl <listen/save/cancel>")
		}
	}
}
