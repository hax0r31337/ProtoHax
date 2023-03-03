package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.Listen
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class ModuleVelocity : CheatModule("Velocity") {

    private var modeValue by listValue("Mode", arrayOf("Simple","Vanilla"), "Vanilla")
    private var horizontalValue by floatValue("Horizontal", 0f, 0f..1f)
    private var verticalValue by floatValue("Vertical", 0f, 0f..1f)

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if (event.packet is SetEntityMotionPacket) {
            if (modeValue == "Vanilla") {
                event.cancel()
            } else {
                event.packet.motion.mul(horizontalValue, verticalValue, horizontalValue)
            }
        }
    }
}
