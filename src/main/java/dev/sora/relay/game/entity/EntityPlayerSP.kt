package dev.sora.relay.game.entity

import com.google.gson.JsonParser
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*
import dev.sora.relay.game.inventory.AbstractInventory
import dev.sora.relay.game.inventory.ContainerInventory
import dev.sora.relay.game.inventory.PlayerInventory
import dev.sora.relay.game.utils.Rotation
import dev.sora.relay.game.utils.constants.EnumFacing
import dev.sora.relay.game.utils.removeNetInfo
import dev.sora.relay.game.utils.toVector3iFloor
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction
import org.cloudburstmc.protocol.bedrock.packet.*
import java.util.*
import kotlin.math.floor

class EntityPlayerSP(private val session: GameSession, override val eventManager: EventManager) : EntityPlayer(0L, 0L, UUID.randomUUID(), ""), Listenable {

    override var runtimeEntityId: Long = 0L
        private set
	override var uniqueEntityId: Long = 0L
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

    var silentRotation: Rotation? = null
	var lastRotationServerside = Rotation(0f, 0f)
		private set

    var prevRotationYaw = 0f
    var prevRotationPitch = 0f
    private var lastTick = 0L
    val renderPartialTicks: Float
        get() {
            val cur = System.currentTimeMillis()
            return (cur - lastTick).coerceIn(0L, 50L) / 50f
        }

	var onGround = false
		set(value) {
			prevOnGround = field
			field = value
		}
	var prevOnGround = false
		private set

    // new introduced "server authoritative" mode
    var blockBreakServerAuthoritative = false
        private set
    var movementServerAuthoritative = true
        private set
	var movementServerRewind = false
		private set
    var inventoriesServerAuthoritative = false
        private set

	val inputData = mutableListOf<PlayerAuthInputData>()
    private val pendingItemInteraction = LinkedList<ItemUseTransaction>()
    private var skipSwings = 0

    override fun rotate(yaw: Float, pitch: Float) {
        this.prevRotationYaw = rotationYaw
        this.prevRotationPitch = rotationPitch
        super.rotate(yaw, pitch)
    }

    fun teleport(x: Float, y: Float, z: Float) {
        move(x, y, z)
        session.netSession.inboundPacket(MovePlayerPacket().apply {
            runtimeEntityId = this@EntityPlayerSP.runtimeEntityId
            position = Vector3f.from(x, y, z)
            rotation = Vector3f.from(rotationPitch, rotationYaw, 0f)
            if (rideEntity != null) {
				ridingRuntimeEntityId = rideEntity!!
				mode = MovePlayerPacket.Mode.HEAD_ROTATION
			} else {
				mode = MovePlayerPacket.Mode.NORMAL
			}
        })
    }

	fun teleport(vec3: Vector3f) {
		teleport(vec3.x, vec3.y, vec3.z)
	}

	override fun reset() {
		super.reset()
		inventory.reset()
		username = ""
	}

	private val disconnectHandler = handle<EventDisconnect> { reset() }

	private val tickHandler = handle<EventTick> {
		lastTick = System.currentTimeMillis()
	}

	private val handlePacketInbound = handle<EventPacketInbound> {
		val packet = it.packet

		if (packet is StartGamePacket) {
			runtimeEntityId = packet.runtimeEntityId
			uniqueEntityId = packet.uniqueEntityId

			blockBreakServerAuthoritative = packet.isServerAuthoritativeBlockBreaking
			movementServerAuthoritative = packet.authoritativeMovementMode != AuthoritativeMovementMode.CLIENT
			movementServerRewind = packet.authoritativeMovementMode == AuthoritativeMovementMode.SERVER_WITH_REWIND
			inventoriesServerAuthoritative = packet.isInventoriesServerAuthoritative

			// override movement authoritative mode
			if (!movementServerAuthoritative) {
				packet.authoritativeMovementMode = AuthoritativeMovementMode.SERVER
			}

			reset()
		} /* else if (packet is RespawnPacket) {
            entityId = packet.runtimeEntityId
        }*/ else if (packet is ContainerOpenPacket) {
			openContainer = if (packet.id.toInt() == 0) {
				return@handle
			} else {
				ContainerInventory(packet.id.toInt(), packet.type).also {
					session.eventManager.emit(EventContainerOpen(session, it))
				}
			}
		} else if (packet is ContainerClosePacket && packet.id.toInt() == openContainer?.containerId) {
			openContainer?.also {
				session.eventManager.emit(EventContainerClose(session, it))
			}
			openContainer = null
		}
		openContainer?.also {
			if (it is ContainerInventory) {
				it.handlePacket(packet)
			}
		}
		onPacket(packet)
	}

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet
		// client still sends MovePlayerPacket sometime on if server-auth movement mode
		if (packet is LoginPacket) {
			// disable packet conversion by default
			movementServerAuthoritative = true
		} else if (packet is MovePlayerPacket) {
			move(packet.position)
			rotate(packet.rotation)

			if (packet.runtimeEntityId != runtimeEntityId) {
				session.chat("runtimeEntityId mismatch, desync occur? (client=${packet.runtimeEntityId}, relay=${runtimeEntityId})")
				runtimeEntityId = packet.runtimeEntityId
			}

			onGround = packet.isOnGround

			session.onTick()
			tickExists = packet.tick
			silentRotation?.let {
				packet.rotation = Vector3f.from(it.pitch, it.yaw, packet.rotation.z)
				silentRotation = null
			}
			lastRotationServerside = Rotation(packet.rotation.y, packet.rotation.x)
		} else if (packet is PlayerAuthInputPacket) {
			move(packet.position)
			rotate(packet.rotation)

			val playerMinY = floor((posY - EYE_HEIGHT) * 1000) / 1000
			onGround = if (playerMinY % 0.125f == 0f) {
				packet.position.add(0f, -EYE_HEIGHT, 0f).toVector3iFloor()
					.let { if (playerMinY % 1 == 0f) it.add(0, -1, 0) else it }
					.let { session.theWorld.getBlockAt(it.x, it.y, it.z).identifier != "minecraft:air" }
			} else prevPosY == posY

			inputData.clear()
			inputData.addAll(packet.inputData)

			session.onTick()

			if (pendingItemInteraction.isNotEmpty() && !packet.inputData.contains(PlayerAuthInputData.PERFORM_ITEM_INTERACTION)) {
				packet.inputData.add(PlayerAuthInputData.PERFORM_ITEM_INTERACTION)
				packet.itemUseTransaction = pendingItemInteraction.pop()
			}

			tickExists = packet.tick
			silentRotation?.let {
				packet.rotation = Vector3f.from(it.pitch, it.yaw, packet.rotation.z)
				silentRotation = null
			}
			lastRotationServerside = Rotation(packet.rotation.y, packet.rotation.x)
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
		} else if (skipSwings > 0 && packet is AnimatePacket && packet.action == AnimatePacket.Action.SWING_ARM) {
			skipSwings--
			event.cancel()
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
            runtimeEntityId = this@EntityPlayerSP.runtimeEntityId
        }.also {
            // send the packet back to client in order to display the swing animation
            if (swingValue == SwingMode.BOTH || swingValue == SwingMode.SERVERSIDE)
                session.sendPacket(it)
            if (swingValue == SwingMode.BOTH || swingValue == SwingMode.CLIENTSIDE) {
                session.netSession.inboundPacket(it)
                skipSwings++
            }
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

    fun useItem(inventoryTransaction: ItemUseTransaction) {
        if (movementServerAuthoritative && inventoriesServerAuthoritative) {
            pendingItemInteraction.add(inventoryTransaction)
        } else {
            session.sendPacket(InventoryTransactionPacket().apply {
                transactionType = InventoryTransactionType.ITEM_USE
                legacyRequestId = inventoryTransaction.legacyRequestId
                actions.addAll(inventoryTransaction.actions)
                actionType = inventoryTransaction.actionType
                blockPosition = inventoryTransaction.blockPosition
                blockFace = inventoryTransaction.blockFace
                hotbarSlot = inventoryTransaction.hotbarSlot
                itemInHand = inventoryTransaction.itemInHand
                playerPosition = inventoryTransaction.playerPosition
                clickPosition = inventoryTransaction.clickPosition
                blockDefinition = inventoryTransaction.blockDefinition
            })
        }
    }

    fun attackEntity(entity: Entity, swingValue: SwingMode = SwingMode.BOTH, sound: Boolean = false, mouseover: Boolean = false) {
		val interactPacket = if (mouseover) InteractPacket().apply {
			action = InteractPacket.Action.MOUSEOVER
			runtimeEntityId = runtimeEntityId
			mousePosition = entity.vec3Position.add((Math.random() * 0.6) - 0.3,
				(Math.random() * 1.8) + (if (entity is EntityPlayer) -EYE_HEIGHT else 0f),
				(Math.random() * 0.6) - 0.3)
		} else null

		interactPacket?.let {
			session.sendPacket(it)
		}

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
            runtimeEntityId = entity.runtimeEntityId
            hotbarSlot = inventory.heldItemSlot
            itemInHand = inventory.hand.removeNetInfo()
            playerPosition = vec3Position
            clickPosition = Vector3f.ZERO
        })
    }

	fun placeBlock(block: Vector3i, facing: EnumFacing) {
		val definition = session.thePlayer.inventory.hand.blockDefinition
		session.netSession.inboundPacket(UpdateBlockPacket().apply {
			blockPosition = block
			this.definition = definition
		})
		session.theWorld.setBlockIdAt(block.x, block.y, block.z, definition?.runtimeId ?: 0)
		session.thePlayer.useItem(ItemUseTransaction().apply {
			actionType = 0
			blockPosition = block.sub(facing.unitVector)
			blockFace = facing.ordinal
			hotbarSlot = session.thePlayer.inventory.heldItemSlot
			itemInHand = session.thePlayer.inventory.hand.removeNetInfo()
			playerPosition = session.thePlayer.vec3Position
			clickPosition = Vector3f.from(Math.random(), Math.random(), Math.random())
			blockDefinition = definition
		})
	}

    enum class SwingMode(override val choiceName: String) : NamedChoice {
        CLIENTSIDE("Client"),
        SERVERSIDE("Server"),
        BOTH("Both"),
        NONE("None")
    }
}
