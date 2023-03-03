package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.Listen
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class ModuleVelocity : CheatModule("Velocity") {

    private var modeValue by listValue("Mode", Mode.values(), Mode.VANILLA)
    private var horizontalValue by floatValue("Horizontal", 0f, 0f..1f)
    private var verticalValue by floatValue("Vertical", 0f, 0f..1f)

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if (event.packet is SetEntityMotionPacket) {
            when (modeValue) {
                Mode.VANILLA -> event.cancel()
                Mode.SIMPLE -> {
                    event.packet.motion = event.packet.motion.mul(horizontalValue, verticalValue, horizontalValue)
                }
            }
        }
    }

    enum class Mode(override val choiceName: String) : NamedChoice {
        SIMPLE("Simple"),
        VANILLA("Vanilla")
    }
}
