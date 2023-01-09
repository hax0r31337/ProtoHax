package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketInbound
import dev.sora.relay.cheat.value.FloatValue
import dev.sora.relay.cheat.value.ListValue

class ModuleVelocity : CheatModule("Velocity") {

    private val modeValue = ListValue("Mode", arrayOf("Cancel", "Simple"), "Vanilla")
    private val horizontalValue = FloatValue("Horizontal", 0f, 0f, 1f)
    private val verticalValue = FloatValue("Vertical", 0f, 0f, 1f)

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