package dev.sora.relay.game.entity

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.SoundEvent
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.Event.Listen
import dev.sora.relay.game.event.Event.Listener
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventPacketOutbound
import java.util.*

class EntityPlayerSP(private val session: GameSession) : EntityPlayer(0L, UUID.randomUUID(), ""), Listener {

    override var entityId: Long = 0L
    var heldItemSlot = 0
        private set

    fun teleport(x: Double, y: Double, z: Double, netSession: RakNetRelaySession) {
        move(x, y, z)
        netSession.inboundPacket(MovePlayerPacket().apply {
            runtimeEntityId = entityId
            position = Vector3f.from(x, y, z)
            rotation = Vector3f.from(rotationPitch, rotationYaw, 0f)
            mode = MovePlayerPacket.Mode.NORMAL
        })
    }

    @Listen
    fun handleServerPacket(event: EventPacketInbound) {
        val packet = event.packet

        if (packet is StartGamePacket) {
            entityId = packet.runtimeEntityId
        } else if (packet is RespawnPacket) {
            entityId = packet.runtimeEntityId
        }
        super.onPacket(packet)
    }

    @Listen
    fun handleClientPacket(event: EventPacketOutbound) {
        val packet = event.packet
        if (packet is MovePlayerPacket) {
            move(packet.position)
            rotate(packet.rotation)
            if (packet.runtimeEntityId != entityId) {
                BasicThing.chat(session, "runtimeEntityId mismatch, desync occur? (client=${packet.runtimeEntityId}, relay=${entityId})")
                entityId = packet.runtimeEntityId
            }
            session.onTick()
            tickExists++
        } else if (packet is PlayerAuthInputPacket) {
            move(packet.position)
            rotate(packet.rotation)
            session.onTick()
            tickExists++
        } else if (packet is PlayerHotbarPacket && packet.containerId == 0) {
            heldItemSlot = packet.selectedHotbarSlot
        } else if (packet is MobEquipmentPacket && packet.runtimeEntityId == entityId) {
            heldItemSlot = packet.hotbarSlot
        }
    }

    fun attackEntity(entity: Entity, swingValue: SwingMode = SwingMode.BOTH) {
        AnimatePacket().apply {
            action = AnimatePacket.Action.SWING_ARM
            runtimeEntityId = session.thePlayer.entityId
        }.also {
            // send the packet back to client in order to display the swing animation
            if (swingValue == SwingMode.BOTH || swingValue == SwingMode.CLIENTSIDE)
                session.netSession.inboundPacket(it)
            if (swingValue == SwingMode.BOTH || swingValue == SwingMode.SERVERSIDE)
                session.sendPacket(it)
        }

        session.sendPacket(LevelSoundEventPacket().apply {
            sound = SoundEvent.ATTACK_STRONG
            position = session.thePlayer.vec3Position
            extraData = -1
            identifier = "minecraft:player"
            isBabySound = false
            isRelativeVolumeDisabled = false
        })

        // attack
        session.sendPacket(InventoryTransactionPacket().apply {
            transactionType = TransactionType.ITEM_USE_ON_ENTITY
            actionType = 1
            runtimeEntityId = entity.entityId
            hotbarSlot = session.thePlayer.heldItemSlot
            itemInHand = ItemData.AIR
            playerPosition = session.thePlayer.vec3Position
            clickPosition = Vector3f.ZERO
        })
    }

    enum class SwingMode {
        CLIENTSIDE,
        SERVERSIDE,
        BOTH,
        NONE
    }

    override fun listen() = true
}