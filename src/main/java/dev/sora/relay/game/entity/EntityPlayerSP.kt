package dev.sora.relay.game.entity

import com.google.gson.JsonParser
import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*
import dev.sora.relay.game.inventory.AbstractInventory
import dev.sora.relay.game.inventory.ContainerInventory
import dev.sora.relay.game.inventory.PlayerInventory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.*
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

    override val inventory = PlayerInventory(this)
    var openContainer: AbstractInventory? = null
        private set

    var silentRotation: Pair<Float, Float>? = null

    var prevRotationYaw = 0f
    var prevRotationPitch = 0f
    private var lastTick = 0L
    val renderPartialTicks: Float
        get() {
            val cur = System.currentTimeMillis()
            return (cur - lastTick).coerceIn(0L, 50L) / 50f
        }

    override fun rotate(yaw: Float, pitch: Float) {
        this.prevRotationYaw = rotationYaw
        this.prevRotationPitch = rotationPitch
        super.rotate(yaw, pitch)
    }

    fun teleport(x: Double, y: Double, z: Double) {
        move(x, y, z)
        session.netSession.inboundPacket(MovePlayerPacket().apply {
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
    fun onTick(event: EventTick) {
        lastTick = System.currentTimeMillis()
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
//                inventory
                return
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
            silentRotation?.let {
                packet.rotation = Vector3f.from(it.first, it.second, packet.rotation.z)
                silentRotation = null
            }
        } else if (packet is PlayerAuthInputPacket) {
            move(packet.position)
            rotate(packet.rotation)
            session.onTick()
            tickExists++
            silentRotation?.let {
                packet.rotation = Vector3f.from(it.first, it.second, packet.rotation.z)
                silentRotation = null
            }
        } else if (packet is LoginPacket) {
            packet.chain.forEach {
                val chainBody = JsonParser.parseString(it.payload.toString()).asJsonObject
                if (chainBody.has("extraData")) {
                    val xData = chainBody.getAsJsonObject("extraData")
                    uuid = UUID.fromString(xData.get("identity").asString)
                    username = xData.get("displayName").asString
                    if (xData.has("XUID")) {
                        xuid = xData.get("XUID").asString
                    }
                }
            }
        } else if (packet is InteractPacket && packet.action == InteractPacket.Action.OPEN_INVENTORY) {
            openContainer = inventory
        }
        inventory.handleClientPacket(packet)
        openContainer?.also {
            if (it is ContainerInventory) {
                it.handleClientPacket(packet)
            }
        }
    }

    fun swing(swingValue: SwingMode = SwingMode.BOTH, sound: Boolean = false) {
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
        if (sound) {
            // this sound will be send to server if no object interacted
            session.sendPacket(LevelSoundEventPacket().apply {
                this.sound = SoundEvent.ATTACK_NODAMAGE
                position = vec3Position
                extraData = -1
                identifier = "minecraft:player"
                isBabySound = false
                isRelativeVolumeDisabled = false
            })
        }
    }

    fun swing(swingValue: String, sound: Boolean = false) {
        swing(getSwingMode(swingValue), sound)
    }

    fun attackEntity(entity: Entity, swingValue: SwingMode = SwingMode.BOTH, sound: Boolean = false) {
        swing(swingValue)

        if (sound) {
            session.sendPacket(LevelSoundEventPacket().apply {
                this.sound = SoundEvent.ATTACK_STRONG
                position = vec3Position
                extraData = -1
                identifier = "minecraft:player"
                isBabySound = false
                isRelativeVolumeDisabled = false
            })
        }

        // attack
        session.sendPacket(InventoryTransactionPacket().apply {
            transactionType = InventoryTransactionType.ITEM_USE_ON_ENTITY
            actionType = 1
            runtimeEntityId = entity.entityId
            hotbarSlot = inventory.heldItemSlot
            itemInHand = ItemData.AIR
            playerPosition = vec3Position
            clickPosition = Vector3f.ZERO
        })
    }
    
    fun attackEntity(entity: Entity, swingValue: String, sound: Boolean = false) {
        attackEntity(entity, getSwingMode(swingValue), sound)
    }

    fun getSwingMode(swingValue: String)
        = when(swingValue) {
            "Both" -> EntityPlayerSP.SwingMode.BOTH
            "Client" -> EntityPlayerSP.SwingMode.CLIENTSIDE
            "Server" -> EntityPlayerSP.SwingMode.SERVERSIDE
            else -> EntityPlayerSP.SwingMode.NONE
        }

    enum class SwingMode {
        CLIENTSIDE,
        SERVERSIDE,
        BOTH,
        NONE
    }

    override fun listen() = true
}