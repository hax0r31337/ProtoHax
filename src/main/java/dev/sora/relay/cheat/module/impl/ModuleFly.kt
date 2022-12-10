package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.data.Ability
import com.nukkitx.protocol.bedrock.data.AbilityLayer
import com.nukkitx.protocol.bedrock.data.PlayerPermission
import com.nukkitx.protocol.bedrock.data.command.CommandPermission
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.RequestAbilityPacket
import com.nukkitx.protocol.bedrock.packet.UpdateAbilitiesPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketInbound
import dev.sora.relay.game.event.impl.EventPacketOutbound

class ModuleFly : CheatModule("Fly") {

    private var launchY = 0.0
    private var canFly = false

    private val abilityPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OPERATOR
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.values())
            abilityValues.addAll(arrayOf(Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES, Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS, Ability.OPERATOR_COMMANDS, Ability.MAY_FLY, Ability.FLY_SPEED, Ability.WALK_SPEED))
            walkSpeed = 0.1f
            flySpeed = 0.25f
        })
    }

    override fun onEnable() {
        session.netSession.inboundPacket(abilityPacket.apply {
            uniqueEntityId = session.thePlayer.entityId
        })
        canFly = false
        launchY = session.thePlayer.posY
    }

//    @Listen
//    fun onTick(event: EventTick) {
//        if (!canFly) return
////        val player = event.session.thePlayer.vec3Position().sub(0f, 1.62f, 0f).floor()
////        event.session.netSession.inboundPacket(UpdateBlockPacket().apply {
////            blockPosition = Vector3i.from(player.x.toInt(), player.y.toInt() - 1, player.z.toInt())
////            dataLayer = 0
////            runtimeId = 1268
////        })
//        val player = event.session.thePlayer
//        move(player, event.session.netSession)
//    }
//
//    private fun move(player: EntityPlayerSP, netSession: RakNetRelaySession) {
//        val pk = MovePlayerPacket().apply {
//            runtimeEntityId = player.entityId
//
//            val yaw = Math.toRadians(player.rotationYaw.toDouble())
//            val value = 2.2f
//
//            position = Vector3f.from(player.posX - sin(yaw) * value, launchY, player.posZ + cos(yaw) * value)
//            player.move(position)
//            rotation = Vector3f.from(player.rotationPitch, player.rotationYaw, 0f)
//            mode = MovePlayerPacket.Mode.NORMAL
//        }
//        netSession.inboundPacket(pk)
//    }

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        if (event.packet is UpdateAbilitiesPacket) {
            event.cancel()
            event.session.netSession.inboundPacket(abilityPacket.apply {
                uniqueEntityId = session.thePlayer.entityId
            })
        }
    }

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        if (event.packet is RequestAbilityPacket && event.packet.ability == Ability.FLYING) {
//            canFly = !canFly
//            if (canFly) {
//                launchY = floor(session.thePlayer.posY) - 0.38
//                event.session.netSession.inboundPacket(EntityEventPacket().apply {
//                    runtimeEntityId = event.session.thePlayer.entityId
//                    type = EntityEventType.HURT
//                    data = 0
//                })
//                val player = event.session.thePlayer
//                event.session.netSession.outboundPacket(MovePlayerPacket().apply {
//                    runtimeEntityId = player.entityId
//                    position = Vector3f.from(player.posX, launchY, player.posZ)
//                    rotation = Vector3f.from(player.rotationPitch, player.rotationYaw, 0f)
//                    mode = MovePlayerPacket.Mode.NORMAL
//                })
//            }
//            event.session.netSession.inboundPacket(abilityPacket.apply {
//                uniqueEntityId = session.thePlayer.entityId
//            })
            event.cancel()
        } else if (event.packet is MovePlayerPacket) {
            event.packet.isOnGround = true
        }
    }
}