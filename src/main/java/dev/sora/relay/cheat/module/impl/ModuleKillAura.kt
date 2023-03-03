package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.ModuleAntiBot.isBot
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.utils.toRotation
import dev.sora.relay.utils.timing.ClickTimer
import kotlin.math.pow

class ModuleKillAura : CheatModule("KillAura") {

    private var cpsValue by intValue("CPS", 7, 1..20)
    private var rangeValue by floatValue("Range", 3.7f, 2f..7f)
    private var attackModeValue by listValue("AttackMode", arrayOf("Single", "Multi"), "Single")
    private var rotationModeValue by listValue("RotationMode", arrayOf("Lock", "None"), "Lock")
    private var swingValue by listValue("Swing", arrayOf("Both", "Client", "Server", "None"), "Both")
    private var swingSoundValue by boolValue("SwingSound", true)
    private var failRateValue by floatValue("FailRate", 0f, 0f..1f)
    private var failSoundValue by boolValue("FailSound", true)

    private val clickTimer = ClickTimer()

    @Listen
    fun onTick(event: EventTick) {
        val session = event.session

        val range = rangeValue.pow(2)
        val entityList = session.theWorld.entityMap.values.filter { it is EntityPlayer && it.distanceSq(session.thePlayer) < range && !it.isBot(session) }
        if (entityList.isEmpty()) return

        val swingMode = session.thePlayer.getSwingMode(swingValue)
        val aimTarget = if (Math.random() <= failRateValue || (cpsValue < 20 && !clickTimer.canClick())) {
            session.thePlayer.swing(swingMode, failSoundValue)
            entityList.first()
        } else {
            when(attackModeValue) {
                "Multi" -> {
                    entityList.forEach { session.thePlayer.attackEntity(it, swingMode, swingSoundValue) }
                    entityList.first()
                }
                else -> (entityList.minByOrNull { it.distanceSq(event.session.thePlayer) } ?: return).also {
                    session.thePlayer.attackEntity(it, swingMode, swingSoundValue)
                }
            }
        }

        if (rotationModeValue == "Lock") {
            session.thePlayer.silentRotation = toRotation(session.thePlayer.vec3Position, aimTarget.vec3Position)
        }

        clickTimer.update(cpsValue, cpsValue + 1)
    }
}