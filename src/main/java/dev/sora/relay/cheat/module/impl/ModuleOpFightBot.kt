package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.ModuleAntiBot.isBot
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ModuleOpFightBot : CheatModule("OPFightBot") {

    private val modeValue = listValue("Mode", arrayOf("Random", "Strafe", "Back"), "Random")
    private val rangeValue = floatValue("Range", 1.5f, 1.5f, 4f)
    private val horizontalSpeedValue = floatValue("HorizontalSpeed", 5f, 1f, 7f)
    private val verticalSpeedValue = floatValue("VerticalSpeed", 4f, 1f, 7f)
    private val strafeSpeedValue = intValue("StrafeSpeed", 20, 10, 90)

    @Listen
    fun onTick(event: EventTick) {
        val session = event.session
        val target = session.theWorld.entityMap.values.filter { it is EntityPlayer && !it.isBot(session) }
            .minByOrNull { it.distanceSq(session.thePlayer) } ?: return
        if(target.distance(session.thePlayer) < 5) {
            val direction = Math.toRadians(when(modeValue.get()) {
                "Random" -> Math.random() * 360
                "Strafe" -> ((session.thePlayer.tickExists * strafeSpeedValue.get()) % 360).toDouble()
                "Back" -> target.rotationYaw + 180.0
                else -> error("no such mode available")
            })
            session.thePlayer.teleport(target.posX - sin(direction) * rangeValue.get(), target.posY + 0.5, target.posZ + cos(direction) * rangeValue.get(), session.netSession)
        } else {
            val direction = atan2(target.posZ - session.thePlayer.posZ, target.posX - session.thePlayer.posX) - Math.toRadians(90.0)
            session.thePlayer.teleport(session.thePlayer.posX - sin(direction) * horizontalSpeedValue.get(),
                target.posY.coerceIn(session.thePlayer.posY - verticalSpeedValue.get(), session.thePlayer.posY + verticalSpeedValue.get()),
                session.thePlayer.posZ + cos(direction) * horizontalSpeedValue.get(), session.netSession)
        }
    }
}