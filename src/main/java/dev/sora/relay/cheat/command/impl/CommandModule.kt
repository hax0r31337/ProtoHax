package dev.sora.relay.cheat.command.impl

import dev.sora.relay.cheat.command.Command
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.*

/**
 * Module command
 * @author SenkJu
 */
class CommandModule(private val module: CheatModule) : Command(module.name.lowercase()) {

    private fun chatSyntax(syntax: String) = chat("Syntax: ${CommandManager.PREFIX}${alias.first()} $syntax")

    /**
     * Execute commands with provided [args]
     */
    override fun exec(args: Array<String>) {
        val valueNames = module.values
            .joinToString(separator = "/") { it.name.lowercase() }

        if (args.isEmpty()) {
            chatSyntax(if (module.values.size == 1) "$valueNames <value>" else "<$valueNames>")
            return
        }

        val value = module.getValue(args[0])

        if (value == null) {
            chatSyntax("<$valueNames>")
            return
        }

        if (args.size < 2) {
            if (value is IntValue || value is FloatValue || value is StringValue || value is BoolValue) {
                chatSyntax("${args[0].lowercase()} <value> (now=${value.value})")
            } else if (value is ListValue) {
                chatSyntax("${args[0].lowercase()} <${value.values.joinToString(separator = "/").lowercase()}> (now=${value.value})")
            }
            return
        }

        try {
            val oldValue = value.value
            value.fromString(args.copyOfRange(2, args.size - 1).joinToString(separator = " "))
            if (oldValue == value.value && value is ListValue) {
                chatSyntax("${args[0].lowercase()} <${value.values.joinToString(separator = "/").lowercase()}>")
                return
            }
            chat("${module.name} ${args[0]} was set to ${value.value}.")
        } catch (e: NumberFormatException) {
            chat("${args[1]} cannot be converted to number!")
        }
    }
}