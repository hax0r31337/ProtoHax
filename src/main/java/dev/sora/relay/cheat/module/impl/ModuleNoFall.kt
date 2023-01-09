package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.game.event.impl.EventTick
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket
import com.nukkitx.protocol.bedrock.data.PlayerActionType
import dev.sora.relay.cheat.value.ListValue


class ModuleNoFall : CheatModule("NoFall") {
    private val modeValue = ListValue("Mode", arrayOf("OnGround","AwayNoGround","Nukkit","CubeCraft"), "OnGround")

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound){
        val packet = event.packet
        val session = event.session

        if(modeValue.get() == "OnGround"){
            if(session.thePlayer.motionY <= -5.5){
                if (packet is MovePlayerPacket){
                    packet.isOnGround = true
                }
            }
        }else if(modeValue.get() == "AwayNoGround"){
            if (packet is MovePlayerPacket){
                packet.isOnGround = false
            }
        }
    }

    @Listen
    fun onTick(event: EventTick){
        val session = event.session

        if(modeValue.get() == "Nukkit"){
            if(session.thePlayer.motionY <= -5.5){
                session.sendPacket(PlayerActionPacket().apply {
                    runtimeEntityId = session.thePlayer.entityId
                    action = PlayerActionType.START_GLIDE
                })
            }
        }else if(modeValue.get() == "CubeCraft"){
            if(session.thePlayer.motionY <= -5.5){

            }
        }
    }
}