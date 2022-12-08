package dev.sora.relay.cheat.command

import com.nukkitx.protocol.bedrock.packet.TextPacket
import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.Listener
import dev.sora.relay.game.event.impl.EventPacketOutbound

class CommandManager(private val session: GameSession) : Listener {

    val commandMap = mutableMapOf<String, Command>()

    fun registerCommand(command: Command) {
        command.alias.forEach {
            commandMap[it] = command
        }
        command.session = session
    }

    fun init() {
    }

    fun exec(msg: String) {
        val args = msg.split(" ").toTypedArray()
        val command = commandMap[args[0].lowercase()]
        if (command == null) {
            BasicThing.chat(session, "Command not found")
            return
        }

        try {
            command.exec(args.copyOfRange(1, args.size))
        } catch (e: Exception) {
            e.printStackTrace()
            BasicThing.chat(session, "An error occurred while executing the command($e)")
        }
    }

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet
        if (packet is TextPacket && packet.message.startsWith(PREFIX)) {
            exec(packet.message.substring(PREFIX.length))
            event.cancel()
        }
    }

    override fun listen() = true

    companion object {
        const val PREFIX = "-"
    }
}