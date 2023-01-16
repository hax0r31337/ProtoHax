package dev.sora.relay.game.entity

import com.google.gson.JsonParser
import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.data.SoundEvent
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*
import dev.sora.relay.game.inventory.AbstractInventory
import dev.sora.relay.game.inventory.ContainerInventory
import dev.sora.relay.game.inventory.PlayerInventory
import dev.sora.relay.utils.base64Decode
import java.util.*

class EntityPlayerSP(private val session: GameSession) : EntityPlayer(0L, UUID.randomUUID(), ""), Listener {

    override var entityId: Long = 0L
        private set
    override var uuid = UUID.randomUUID()
        private set
    override var username = ""
        private set
    var xuid = ""
        private set

    override val inventory = PlayerInventory()
    var openContainer: AbstractInventory? = null
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
    fun onDisconnect(event: EventDisconnect) {
        reset()
    }

    override fun reset() {
        super.reset()
        inventory.reset()
        username = ""
    }

    @Listen
    fun handleServerPacket(event: EventPacketInbound) {
        val packet = event.packet

        if (packet is StartGamePacket) {
            entityId = packet.runtimeEntityId
            reset()
        } /* else if (packet is RespawnPacket) {
            entityId = packet.runtimeEntityId
        }*/ else if (packet is ContainerOpenPacket) {
            openContainer = if (packet.id.toInt() == 0) {
                inventory
            } else {
                ContainerInventory(packet.id.toInt(), packet.type).also {
                    session.eventManager.emit(EventContainerOpen(session, it))
                }
            }
        } else if (packet is ContainerClosePacket && packet.id.toInt() == openContainer?.containerId) {
            openContainer?.also {
                session.eventManager.emit(EventContainerOpen(session, it))
            }
            openContainer = null
        }
        openContainer?.also {
            if (it is ContainerInventory) {
                it.handlePacket(packet)
            }
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
        } else if (packet is LoginPacket) {
            val body = JsonParser.parseString(packet.chainData.toString()).asJsonObject.getAsJsonArray("chain")
            for (chain in body) {
                val chainBody = JsonParser.parseString(base64Decode(chain.asString.split(".")[1]).toString(Charsets.UTF_8)).asJsonObject
                if (chainBody.has("extraData")) {
                    val xData = chainBody.getAsJsonObject("extraData")
                    uuid = UUID.fromString(xData.get("identity").asString)
                    username = xData.get("displayName").asString
                    if (xData.has("XUID")) {
                        xuid = xData.get("XUID").asString
                    }
                }
            }
        }
        inventory.handleClientPacket(packet)
        openContainer?.also {
            if (it is ContainerInventory) {
                it.handleClientPacket(packet)
            }
        }
    }

    fun swing(swingValue: SwingMode = SwingMode.BOTH) {
        AnimatePacket().apply {
            action = AnimatePacket.Action.SWING_ARM
            runtimeEntityId = entityId
        }.also {
            // send the packet back to client in order to display the swing animation
            if (swingValue == SwingMode.BOTH || swingValue == SwingMode.CLIENTSIDE)
                session.netSession.inboundPacket(it)
            if (swingValue == SwingMode.BOTH || swingValue == SwingMode.SERVERSIDE)
                session.sendPacket(it)
        }
    }

    fun attackEntity(entity: Entity, swingValue: SwingMode = SwingMode.BOTH) {
        swing(swingValue)

        session.sendPacket(LevelSoundEventPacket().apply {
            sound = SoundEvent.ATTACK_STRONG
            position = vec3Position
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
            hotbarSlot = inventory.heldItemSlot
            itemInHand = ItemData.AIR
            playerPosition = vec3Position
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