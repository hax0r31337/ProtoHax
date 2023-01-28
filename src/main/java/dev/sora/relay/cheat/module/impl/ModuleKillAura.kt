package dev.sora.relay.cheat.module.impl

import com.nukkitx.math.vector.Vector3f
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.ModuleAntiBot.isBot
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.cheat.value.FloatValue
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import dev.sora.relay.utils.timing.ClickTimer
import java.lang.Math.atan2
import java.lang.Math.sqrt
import kotlin.math.pow

class ModuleKillAura : CheatModule("KillAura") {

    private val cpsValue = IntValue("CPS", 7, 1, 20)
    private val rangeValue = FloatValue("Range", 3.7f, 2f, 7f)
    private val attackModeValue = ListValue("AttackMode", arrayOf("Single", "Multi"), "Single")
    private val rotationModeValue = ListValue("RotationMode", arrayOf("Lock", "None"), "Lock")
    private val swingValue = ListValue("Swing", arrayOf("Both", "Client", "Server", "None"), "Both")
    private val swingSoundValue = BoolValue("SwingSound", true)
    private val failRateValue = FloatValue("FailRate", 0f, 0f, 1f)
    private val failSoundValue = BoolValue("FailSound", true)

    private val clickTimer = ClickTimer()

    @Listen
    fun onTick(event: EventTick) {
        if (cpsValue.get() < 20 && !clickTimer.canClick())
            return

        val session = event.session

        val range = rangeValue.get().pow(2)
        val entityList = session.theWorld.entityMap.values.filter { it is EntityPlayer && it.distanceSq(session.thePlayer) < range && !it.isBot(session) }
        if (entityList.isEmpty()) return

        val swingMode = when(swingValue.get()) {
            "Both" -> EntityPlayerSP.SwingMode.BOTH
            "Client" -> EntityPlayerSP.SwingMode.CLIENTSIDE
            "Server" -> EntityPlayerSP.SwingMode.SERVERSIDE
            else -> EntityPlayerSP.SwingMode.NONE
        }
        val aimTarget = if (Math.random() <= failRateValue.get()) {
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
            session.thePlayer.silentRotation = toRotation(session.thePlayer.vec3Position, aimTarget.vec3Position).let {
                (it.first - session.thePlayer.rotationYaw) * 0.8f + session.thePlayer.rotationYaw to it.second
            }
        }

        clickTimer.update(cpsValue.get(), cpsValue.get() + 1)
    }

    private fun toRotation(from: Vector3f, to: Vector3f): Pair<Float, Float> {
        val diffX = (to.x - from.x).toDouble()
        val diffY = (to.y - from.y).toDouble()
        val diffZ = (to.z - from.z).toDouble()
        return Pair(
            ((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat()),
            (Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f)
        )
    }
}