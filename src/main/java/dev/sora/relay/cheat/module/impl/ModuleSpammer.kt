package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.TextPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.cheat.value.StringValue
import dev.sora.relay.game.event.Event.Listen
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.utils.getRandomString
import dev.sora.relay.utils.timing.TheTimer
import kotlin.random.Random

class ModuleSpammer : CheatModule("Spammer") {

    private val delayValue = IntValue("Delay", 5000, 500, 10000)
    private val messageValue = StringValue("Message", "[!] I'm using ProtoHax t<dot>me/protohax")

    private val spamTimer = TheTimer()

    @Listen
    fun onTick(event: EventTick) {
        if (spamTimer.hasTimePassed(delayValue.get())) {
            event.session.sendPacket(TextPacket().apply {
                type = TextPacket.Type.CHAT
                xuid = event.session.xuid
                sourceName = event.session.displayName
                platformChatId = ""
                message = "${messageValue.get()} >${getRandomString(10 + Random.nextInt(5))}<"
            })
            spamTimer.reset()
        }
    }
}