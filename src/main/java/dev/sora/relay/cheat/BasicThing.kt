package dev.sora.relay.cheat

import com.nukkitx.protocol.bedrock.packet.TextPacket
import dev.sora.relay.game.GameSession
import dev.sora.relay.utils.logInfo

abstract class BasicThing {

    lateinit var session: GameSession

    protected fun chat(msg: String) {
        chat(session, msg)
    }

    companion object {
        fun chat(session: GameSession, msg: String) {
            logInfo("chat >> $msg")
            if (!session.netSessionInitialized) return
            session.netSession.inboundPacket(TextPacket().apply {
                type = TextPacket.Type.RAW
                isNeedsTranslation = false
                message = "[§9§lProtoHax§r] $msg"
                xuid = ""
                sourceName = ""
            })
        }
    }
}