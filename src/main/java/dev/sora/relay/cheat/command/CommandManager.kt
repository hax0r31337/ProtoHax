package dev.sora.relay.cheat.command

import dev.sora.relay.cheat.command.impl.CommandModule
import dev.sora.relay.cheat.command.impl.CommandToggle
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.EventManager
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.Listenable
import dev.sora.relay.game.event.handle
import dev.sora.relay.utils.logError
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

class CommandManager(private val session: GameSession) : Listenable {

    val commandMap = mutableMapOf<String, Command>()

    fun registerCommand(command: Command) {
        command.alias.forEach {
            commandMap[it] = command
        }
    }

    fun init(moduleManager: ModuleManager) {
        registerCommand(CommandToggle(moduleManager))
        moduleManager.modules.forEach {
            if (it.values.isEmpty()) return@forEach
            registerCommand(CommandModule(it))
        }
    }

    fun exec(msg: String) {
        val args = msg.split(" ").toTypedArray()
        val command = commandMap[args[0].lowercase()]
        if (command == null) {
            session.chat("Command not found")
            return
        }

        try {
            command.exec(args.copyOfRange(1, args.size), session)
        } catch (e: Exception) {
            logError("execute command", e)
            session.chat("An error occurred while executing the command($e)")
        }
    }

	private val handlePacketOutbound = handle<EventPacketOutbound> {
		if (packet is TextPacket && packet.message.startsWith(PREFIX)) {
			exec(packet.message.substring(PREFIX.length))
			cancel()
		}
	}

	override val eventManager: EventManager
		get() = session.eventManager

    companion object {
        const val PREFIX = "-"
    }
}
