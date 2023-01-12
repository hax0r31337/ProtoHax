package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.EventPacketOutbound

class ModuleCriticals : CheatModule("Criticals") {
    private val modeValue = ListValue("Mode", arrayOf("HYT"), "HYT")

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound){
        if(event.packet is InventoryTransactionPacket){
            if(event.packet.transactionType == TransactionType.ITEM_USE_ON_ENTITY && event.packet.actionType == 1){ //Attack
                if(modeValue.get() == "HYT"){
                    event.session.netSession.outboundPacket(MovePlayerPacket().apply {
                        runtimeEntityId = event.session.thePlayer.entityId
                        position = event.session.thePlayer.vec3Position.add(0f, 0.01f, 0f)
                        rotation = event.session.thePlayer.vec3Rotation
                        isOnGround = false
                    })

                    event.session.netSession.outboundPacket(MovePlayerPacket().apply {
                        runtimeEntityId = event.session.thePlayer.entityId
                        position = event.session.thePlayer.vec3Position.add(0f, 0.0001f, 0f)
                        rotation = event.session.thePlayer.vec3Rotation
                        isOnGround = false
                    })
                }
            }
        }
    }
}