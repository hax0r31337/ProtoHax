package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketInbound
import dev.sora.relay.cheat.value.FloatValue

class ModuleVelocity : CheatModule("Velocity") {
    private val horizontalValue = FloatValue("Horizontal", 0f, 0f, 1f)
    private val verticalValue = FloatValue("Vertical", 0f, 0f, 1f)

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if (event.packet is SetEntityMotionPacket) {
            event.packet.motion.mul(horizontalValue.get(),verticalValue.get(),horizontalValue.get())
            //event.cancel()
        }
    }
}