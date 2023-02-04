package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.Listen

class ModuleVelocity : CheatModule("Velocity") {

    private val modeValue = listValue("Mode", arrayOf("Simple","Vanilla"), "Vanilla")
    private val horizontalValue = floatValue("Horizontal", 0f, 0f, 1f)
    private val verticalValue = floatValue("Vertical", 0f, 0f, 1f)

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if (event.packet is SetEntityMotionPacket) {
            if (modeValue.get() == "Vanilla") {
                event.cancel()
            } else {
                event.packet.motion.mul(horizontalValue.get(),verticalValue.get(),horizontalValue.get())
            }
        }
    }
}
