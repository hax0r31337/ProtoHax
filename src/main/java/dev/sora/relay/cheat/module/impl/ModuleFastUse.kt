package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.EventPacketInbound
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType
import com.nukkitx.protocol.bedrock.data.inventory.ItemData

class ModuleFastUse : CheatModule("FastUse") {
    @Listen
    fun onPacketOutbound(event: EventPacketInbound){
        val packet = event.packet
        if(packet is InventoryTransactionPacket){
            if(packet.transactionType == TransactionType.ITEM_USE){
                for(i in 0 until 7){
                    event.session.netSession.outboundPacket(EntityEventPacket().apply {
                        runtimeEntityId = event.session.thePlayer.entityId
                        type = EntityEventType.EATING_ITEM
                        data = 1
                    })
                }

                event.session.netSession.outboundPacket(InventoryTransactionPacket().apply {
                    runtimeEntityId = event.session.thePlayer.entityId
                    transactionType = TransactionType.ITEM_USE
                    actionType = 1
                    hotbarSlot = session.thePlayer.heldItemSlot
                    itemInHand = ItemData.AIR
                    playerPosition = session.thePlayer.vec3Position
                    headPosition = null
                    clickPosition = null
                })
            }
        }
    }
}