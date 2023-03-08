package dev.sora.relay.cheat.command.impl

import dev.sora.relay.cheat.command.Command
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.world.leveldb.LevelDBLevelData
import dev.sora.relay.game.world.leveldb.LevelDBWorld
import java.io.File

class CommandDownloadWorld : Command("wdl") {

	override fun exec(args: Array<String>, session: GameSession) {
		val folder = File("./level").also {
			it.mkdirs()
		}

		// save chunks
		val world = LevelDBWorld(File(folder, "db"))
		session.theWorld.chunks.values.forEach {
			world.saveChunk(it, session.theWorld.dimension)
		}
		world.close()

		// save level dat
		val levelData = LevelDBLevelData(session.netSession.codec.protocolVersion, session.netSession.codec.minecraftVersion)
		File(folder, "level.dat").writeBytes(levelData.toBytes())
	}
}
