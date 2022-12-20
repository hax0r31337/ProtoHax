package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.ModuleAntiBot.isBot
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventTick
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ModuleOpFightBot : CheatModule("OPFightBot") {

    @Listen
    fun onTick(event: EventTick) {
        val session = event.session
        val target = session.theWorld.entityMap.values.filter { it is EntityPlayer && !it.isBot(session) }
            .minByOrNull { it.distanceSq(session.thePlayer) } ?: return
        if(target.distance(session.thePlayer) < 5) {
            val direction = Math.toRadians(Math.random() * 180)
            session.thePlayer.teleport(target.posX - sin(direction) * 1.5, target.posY + 0.5, target.posZ + cos(direction) * 1.5, session.netSession)
        } else {
            val direction = atan2(target.posZ - session.thePlayer.posZ, target.posX - session.thePlayer.posX) - Math.toRadians(90.0)
            session.thePlayer.teleport(session.thePlayer.posX - sin(direction) * 5,
                target.posY.coerceIn(session.thePlayer.posY - 4, session.thePlayer.posY + 4),
                session.thePlayer.posZ + cos(direction) * 5, session.netSession)
        }
    }
}