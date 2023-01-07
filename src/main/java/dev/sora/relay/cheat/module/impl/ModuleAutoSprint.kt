package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket
import com.nukkitx.protocol.bedrock.data.PlayerActionType
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventTick
import dev.sora.relay.game.event.impl.EventPacketOutbound

class ModuleAutoSprint : CheatModule("AutoSprint") {
    private var isSprint = false

    @Listen
    fun onTick(event: EventTick){
        val session = event.session

        if(session.thePlayer.motionX > 0.01f || session.thePlayer.motionZ > 0.01f){
            if(!isSprint){
                session.netSession.outboundPacket(PlayerActionPacket().apply {
                    runtimeEntityId = session.thePlayer.entityId
                    action = PlayerActionType.START_SPRINT
                })
                isSprint = true
            }
        }else {
            isSprint = false
        }
    }

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound){
        if (event.packet is PlayerActionPacket){
            if(event.packet.action == PlayerActionType.STOP_SPRINT){
                isSprint = false
            }
        }
    }
}