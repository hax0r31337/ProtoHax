package dev.sora.relay.cheat.module.impl

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.AnimatePacket
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventTick

class ModuleKillAura : CheatModule("KillAura") {

    @Listen
    fun onTick(event: EventTick) {
        val session = event.session

        val entity = session.theWorld.entityMap.values.filter { it is EntityPlayer && it.distanceSq(session.thePlayer) < 15 }
            .firstOrNull() ?: return

        // swing
        AnimatePacket().apply {
            action = AnimatePacket.Action.SWING_ARM
            runtimeEntityId = session.thePlayer.entityId
        }.also {
            // send the packet back to client in order to display the swing animation
            session.netSession.inboundPacket(it)
            session.netSession.outboundPacket(it)
        }

        // attack
        session.netSession.outboundPacket(InventoryTransactionPacket().apply {
            transactionType = TransactionType.ITEM_USE_ON_ENTITY
            actionType = 1
            runtimeEntityId = entity.entityId
            hotbarSlot = 0
            itemInHand = ItemData.AIR
            playerPosition = session.thePlayer.vec3Position()
            clickPosition = Vector3f.ZERO
        })
    }
}