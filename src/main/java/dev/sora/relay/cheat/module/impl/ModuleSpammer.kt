package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.TextPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventTick
import dev.sora.relay.utils.getRandomString
import java.util.*

class ModuleSpammer : CheatModule("Spammer", defaultOn = false) {

    private var ticks = 0

    @Listen
    fun onTick(event: EventTick) {
        ticks++
        if (ticks % 150 == 0) {
            event.session.netSession.outboundPacket(TextPacket().apply {
                type = TextPacket.Type.CHAT
                xuid = event.session.xuid
                sourceName = event.session.displayName
                platformChatId = ""
                message = "LiquidBounce Client | liquidbounce(.net) >${getRandomString(10 + Random().nextInt(5))}<"
//                message = ""
            })
            ticks = 0
        }
    }
}