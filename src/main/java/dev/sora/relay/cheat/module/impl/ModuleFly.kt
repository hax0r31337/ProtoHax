package dev.sora.relay.cheat.module.impl

import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType
import org.cloudburstmc.protocol.bedrock.packet.*
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class ModuleFly : CheatModule("Fly") {

    private val modeValue = listValue("Mode", arrayOf("Vanilla", "Jetpack", "Mineplex"), "Vanilla")
    private val speedValue = floatValue("Speed", 1.5f, 0.1f, 5f)
    private val mineplexDirectValue = boolValue("MineplexDirect", false)
    private val mineplexMotionValue = boolValue("MineplexMotion", false)

    private var launchY = 0.0
    private var canFly = false

    private val abilityPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.values())
            abilityValues.addAll(arrayOf(Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES, Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS, Ability.OPERATOR_COMMANDS, Ability.MAY_FLY, Ability.FLY_SPEED, Ability.WALK_SPEED))
            walkSpeed = 0.1f
            flySpeed = 0.15f
        })
    }

    override fun onEnable() {
        canFly = false
        if (modeValue.get() == "Mineplex" && mineplexDirectValue.get()) {
            canFly = true
        }
        launchY = session.thePlayer.posY
    }

    @Listen
    fun onTick(event: EventTick) {
        val session = event.session

        if (modeValue.get() == "Mineplex") {
            session.netSession.inboundPacket(abilityPacket.apply {
                uniqueEntityId = session.thePlayer.entityId
            })
            if (!canFly) return
            val player = session.thePlayer
            val yaw = Math.toRadians(player.rotationYaw.toDouble())
            val value = speedValue.get()
            if (mineplexMotionValue.get()) {
                session.netSession.inboundPacket(SetEntityMotionPacket().apply {
                    runtimeEntityId = session.thePlayer.entityId
                    motion = Vector3f.from(-sin(yaw) * value, 0.0, +cos(yaw) * value)
                })
            } else {
                player.teleport(player.posX - sin(yaw) * value, launchY, player.posZ + cos(yaw) * value)
            }
        } else if (modeValue.get() == "Vanilla" && !canFly) {
            canFly = true
            session.netSession.inboundPacket(abilityPacket.apply {
                uniqueEntityId = session.thePlayer.entityId
            })
        } else if(modeValue.get() == "Jetpack"){
            session.netSession.inboundPacket(SetEntityMotionPacket().apply {
                runtimeEntityId = session.thePlayer.entityId

                val calcYaw: Double = (session.thePlayer.rotationYawHead + 90) * (PI / 180)
                val calcPitch: Double = (session.thePlayer.rotationPitch) * -(PI / 180)

                motion = Vector3f.from(
                    cos(calcYaw) * cos(calcPitch) * speedValue.get(),
                    sin(calcPitch) * speedValue.get(),
                    sin(calcYaw) * cos(calcPitch) * speedValue.get()
                )
            })
        }
    }

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if (event.packet is UpdateAbilitiesPacket) {
            event.cancel()
            event.session.netSession.inboundPacket(abilityPacket.apply {
                uniqueEntityId = event.session.thePlayer.entityId
            })
        } else if (event.packet is StartGamePacket) {
            event.session.netSession.inboundPacket(abilityPacket.apply {
                uniqueEntityId = event.session.thePlayer.entityId
            })
        }
    }

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        if (modeValue.get() == "Mineplex") {
            if (event.packet is RequestAbilityPacket && event.packet.ability == Ability.FLYING) {
                canFly = !canFly
                if (canFly) {
                    launchY = floor(session.thePlayer.posY) - 0.38
                    event.session.sendPacketToClient(EntityEventPacket().apply {
                        runtimeEntityId = event.session.thePlayer.entityId
                        type = EntityEventType.HURT
                        data = 0
                    })
                    val player = event.session.thePlayer
                    repeat(5) {
                        event.session.sendPacket(MovePlayerPacket().apply {
                            runtimeEntityId = player.entityId
                            position = Vector3f.from(player.posX, launchY, player.posZ)
                            rotation = Vector3f.from(player.rotationPitch, player.rotationYaw, 0f)
                            mode = MovePlayerPacket.Mode.NORMAL
                        })
                    }
                }
                event.session.netSession.inboundPacket(abilityPacket.apply {
                    uniqueEntityId = session.thePlayer.entityId
                })
                event.cancel()
            } else if (event.packet is MovePlayerPacket && canFly) {
                event.packet.isOnGround = true
                event.packet.position = event.packet.position.let {
                    Vector3f.from(it.x, launchY.toFloat(), it.z)
                }
            }
        } else {
            if (event.packet is RequestAbilityPacket && event.packet.ability == Ability.FLYING) {
                event.cancel()
            }
        }
    }
}