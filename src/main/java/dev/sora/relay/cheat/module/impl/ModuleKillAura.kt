package dev.sora.relay.cheat.module.impl

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.AnimatePacket
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityUnknown
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.game.event.impl.EventTick
import java.lang.Math.atan2
import java.lang.Math.sqrt

class ModuleKillAura : CheatModule("KillAura") {

    private var rotation: Pair<Float, Float>? = null
    private var lastHit = 0

    @Listen
    fun onTick(event: EventTick) {
        val session = event.session

        val entity = session.theWorld.entityMap.values.filter { it is EntityPlayer && it.distanceSq(session.thePlayer) < 20 }
            .firstOrNull() ?: return

//        rotation = toRotation(session.thePlayer.vec3Position(), entity.vec3Position().add(0f, 1f, 0f)).let {
//            (it.first - session.thePlayer.rotationYaw) * 0.8f + session.thePlayer.rotationYaw to it.second
//        }

        if (lastHit != 0) {
            lastHit--
            return
        }
        lastHit = 3

        if (entity is EntityUnknown) {
            println(entity.type)
        }

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
            hotbarSlot = event.session.thePlayer.heldItemSlot
            itemInHand = ItemData.AIR
            playerPosition = session.thePlayer.vec3Position()
            clickPosition = Vector3f.ZERO
        })
    }

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val rotation = rotation ?: return
        val packet = event.packet

        if (packet is PlayerAuthInputPacket) {
            packet.rotation = Vector3f.from(rotation.first, rotation.second, packet.rotation.z)
            this.rotation = null
        } else if (packet is MovePlayerPacket) {
            packet.rotation = Vector3f.from(rotation.first, rotation.second, packet.rotation.z)
            this.rotation = null
        }
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