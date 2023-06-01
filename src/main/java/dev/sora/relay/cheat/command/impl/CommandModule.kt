package dev.sora.relay.cheat.command.impl

import dev.sora.relay.cheat.command.Command
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.*
import dev.sora.relay.game.GameSession
import dev.sora.relay.utils.logError

/**
 * Module command
 * @author SenkJu
 */
class CommandModule(private val module: CheatModule) : Command(module.name.lowercase()) {

    private fun GameSession.chatSyntax(syntax: String) = chat("Syntax: ${CommandManager.PREFIX}${alias.first()} $syntax")

    /**
     * Execute commands with provided [args]
     */
    override fun exec(args: Array<String>, session: GameSession) {
        val valueNames = module.values
            .joinToString(separator = "/") { it.name }

        if (args.isEmpty()) {
            session.chatSyntax(if (module.values.size == 1) "$valueNames <value>" else "<$valueNames>")
            return
        }

        val value = module.getValue(args[0])

        if (value == null) {
            session.chatSyntax("<$valueNames>")
            return
        }

        if (args.size < 2) {
            if (value is IntValue || value is FloatValue || value is StringValue || value is BoolValue) {
                session.chatSyntax("${args[0].lowercase()} <value> (now=${value.value})")
            } else if (value is ListValue) {
				session.chatSyntax("${args[0].lowercase()} <${value.values.map { (it as NamedChoice).choiceName }.joinToString(separator = "/")}> (now=${(value.value as NamedChoice).choiceName})")
            }
            return
        }

		val source = args.copyOfRange(1, args.size).joinToString(separator = " ")
        try {
            value.fromString(source)
            session.chat("${module.name} ${value.name} was set to ${if (value.value is NamedChoice) (value.value as NamedChoice).choiceName else value.value}.")
        } catch (e: Throwable) {
			logError("value.fromString", e)
            session.chat("Unable to parse value \"$source\" for value ${value.name}")
        }
    }
}
