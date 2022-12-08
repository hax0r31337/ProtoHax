package dev.sora.relay.cheat

import com.nukkitx.protocol.bedrock.packet.TextPacket
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.Listener

abstract class CheatModule(val name: String,
                           defaultOn: Boolean = false,
                           private val canToggle: Boolean = true) : Listener {

    lateinit var session: GameSession

    var state = defaultOn
        set(state) {
            if (field == state) return

            if (!canToggle) {
                onEnable()
                return
            }
            field = state

            if (state) {
                onEnable()
            } else {
                onDisable()
            }
        }

    open fun onEnable() {}

    open fun onDisable() {}

    protected fun chat(msg: String) {
        session.netSession.inboundPacket(TextPacket().apply {
            type = TextPacket.Type.RAW
            isNeedsTranslation = false
            message = "[§9§lProtoHax§r] $msg"
        })
    }

    override fun listen() = state
}