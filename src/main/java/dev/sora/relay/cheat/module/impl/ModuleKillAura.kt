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

    private val cpsValue = intValue("CPS", 7, 1, 20)
    private val rangeValue = floatValue("Range", 3.7f, 2f, 7f)
    private val attackModeValue = listValue("AttackMode", arrayOf("Single", "Multi"), "Single")
    private val rotationModeValue = listValue("RotationMode", arrayOf("Lock", "None"), "Lock")
    private val swingValue = listValue("Swing", arrayOf("Both", "Client", "Server", "None"), "Both")
    private val swingSoundValue = boolValue("SwingSound", true)
    private val failRateValue = floatValue("FailRate", 0f, 0f, 1f)
    private val failSoundValue = boolValue("FailSound", true)

    private val clickTimer = ClickTimer()

    @Listen
    fun onTick(event: EventTick) {

        val session = event.session

        val range = rangeValue.get().pow(2)
        val entityList = session.theWorld.entityMap.values.filter { it is EntityPlayer && it.distanceSq(session.thePlayer) < range && !it.isBot(session) }
        if (entityList.isEmpty()) return

        val swingMode = session.thePlayer.getSwingMode(swingValue.get())
        val aimTarget = if (Math.random() <= failRateValue.get() || (cpsValue.get() < 20 && !clickTimer.canClick())) {
            session.thePlayer.swing(swingMode, failSoundValue.get())
            entityList.first()
        } else {
            when(attackModeValue.get()) {
                "Multi" -> {
                    entityList.forEach { session.thePlayer.attackEntity(it, swingMode, swingSoundValue.get()) }
                    entityList.first()
                }
                else -> (entityList.minByOrNull { it.distanceSq(event.session.thePlayer) } ?: return).also {
                    session.thePlayer.attackEntity(it, swingMode, swingSoundValue.get())
                }
            }
        }

        if (rotationModeValue.get() == "Lock") {
            session.thePlayer.silentRotation = toRotation(session.thePlayer.vec3Position, aimTarget.vec3Position)
        }

        clickTimer.update(cpsValue.get(), cpsValue.get() + 1)
    }
}