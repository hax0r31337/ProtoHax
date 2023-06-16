package dev.sora.relay.game

import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.*
import dev.sora.relay.game.management.BlobCacheManager
import dev.sora.relay.game.registry.BlockMapping
import dev.sora.relay.game.registry.ItemMapping
import dev.sora.relay.game.registry.LegacyBlockMapping
import dev.sora.relay.game.world.WorldClient
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.*
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class GameSession : MinecraftRelayPacketListener {

	val eventManager = EventManager()

    val thePlayer = EntityPlayerSP(this, eventManager)
    val theWorld = WorldClient(this, eventManager)

    val cacheManager = BlobCacheManager(eventManager)

    lateinit var netSession: MinecraftRelaySession

	var itemMapping = ItemMapping(mutableMapOf())
		private set
    var blockMapping = BlockMapping(mutableMapOf(), 0)
        private set
    var legacyBlockMapping: Lazy<LegacyBlockMapping> = lazy { LegacyBlockMapping(emptyMap()) }
        private set

    val netSessionInitialized: Boolean
        get() = this::netSession.isInitialized

	private var lastStopBreak = false

	val scope = CoroutineScope(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher() + SupervisorJob())

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

		if (packet is StartGamePacket) {
			if (packet.itemDefinitions.isNotEmpty()) {
				itemMapping.registerCustomItems(packet.itemDefinitions)
			}
//			if (packet.blockProperties.isNotEmpty()) {
//				blockMapping.registerCustomBlocks(packet.blockProperties)
//			}
		}

        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        val event = EventPacketOutbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        if (packet is LoginPacket) {
			val protocolVersion = packet.protocolVersion

			while (netSession.client == null) {
				Thread.sleep(10L) // wait client connect
			}

			val blockTask = thread {
				val blockDefinitions = BlockMapping.Provider.craftMapping(protocolVersion)
				netSession.peer.codecHelper.blockDefinitions = blockDefinitions
				netSession.client!!.peer.codecHelper.blockDefinitions = blockDefinitions

				blockMapping = blockDefinitions
			}

			val itemDefinitions = ItemMapping.Provider.craftMapping(protocolVersion)
			netSession.peer.codecHelper.itemDefinitions = itemDefinitions
			netSession.client!!.peer.codecHelper.itemDefinitions = itemDefinitions

			itemMapping = itemDefinitions

			legacyBlockMapping = lazy { LegacyBlockMapping.Provider.craftMapping(protocolVersion) }

			if (blockTask.isAlive) {
				blockTask.join()
			}
        } else if (!thePlayer.movementServerAuthoritative && packet is PlayerAuthInputPacket) {
			convertAuthInput(packet)?.also { netSession.outboundPacket(it) }
			return false
		}

        return true
    }

    override fun onDisconnect(client: Boolean, reason: String) {
        eventManager.emit(EventDisconnect(this, client, reason))
    }

    fun onTick() {
        eventManager.emit(EventTick(this))
    }

    fun sendPacket(packet: BedrockPacket) {
        val event = EventPacketOutbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return
        }

        netSession.outboundPacket(packet)
    }

    fun sendPacketToClient(packet: BedrockPacket) {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return
        }
        netSession.inboundPacket(packet)
    }

	fun chat(msg: String) {
		logInfo("chat >> $msg")
		if (!netSessionInitialized) return
		sendPacketToClient(TextPacket().apply {
			type = TextPacket.Type.RAW
			isNeedsTranslation = false
			message = "[$COLORED_NAME] $msg"
			xuid = ""
			sourceName = ""
		})
	}

	private val inputDataConversionMap by lazy { mapOf(
		PlayerAuthInputData.START_SPRINTING to PlayerActionType.START_SPRINT, PlayerAuthInputData.STOP_SPRINTING to PlayerActionType.STOP_SPRINT,
		PlayerAuthInputData.START_SNEAKING to PlayerActionType.START_SNEAK, PlayerAuthInputData.STOP_SNEAKING to PlayerActionType.STOP_SNEAK,
		PlayerAuthInputData.START_SWIMMING to PlayerActionType.START_SWIMMING, PlayerAuthInputData.STOP_SWIMMING to PlayerActionType.STOP_SWIMMING,
		PlayerAuthInputData.START_GLIDING to PlayerActionType.START_GLIDE, PlayerAuthInputData.STOP_GLIDING to PlayerActionType.STOP_GLIDE,
		PlayerAuthInputData.START_JUMPING to PlayerActionType.JUMP) }

	private fun convertAuthInput(packet: PlayerAuthInputPacket): MovePlayerPacket? {
		packet.playerActions.forEach { action ->
			netSession.outboundPacket(PlayerActionPacket().apply {
				runtimeEntityId = thePlayer.runtimeEntityId
				this.action = action.action
				blockPosition = action.blockPosition ?: Vector3i.ZERO
				resultPosition = Vector3i.ZERO
				face = action.face
			})
			if (action.action == PlayerActionType.STOP_BREAK) {
				lastStopBreak = true
			} else if (lastStopBreak && action.action == PlayerActionType.CONTINUE_BREAK) {
				netSession.outboundPacket(InventoryTransactionPacket().apply {
					transactionType = InventoryTransactionType.ITEM_USE
					actionType = 2
					blockPosition = action.blockPosition
					blockFace = action.face
					itemInHand = ItemData.AIR
					playerPosition = thePlayer.vec3Position
					clickPosition = Vector3f.ZERO
					blockDefinition = blockMapping.getDefinition(0)
				})
				lastStopBreak = false
			}
		}

		inputDataConversionMap.forEach { (k, v) ->
			if (packet.inputData.contains(k)) {
				netSession.outboundPacket(PlayerActionPacket().apply {
					runtimeEntityId = thePlayer.runtimeEntityId
					action = v
					blockPosition = Vector3i.ZERO
					resultPosition = Vector3i.ZERO
				})
			}
		}

		var mode = MovePlayerPacket.Mode.NORMAL
		if (packet.position.x == thePlayer.prevPosX && packet.position.y == thePlayer.prevPosY && packet.position.z == thePlayer.prevPosZ) {
			if (packet.rotation.x == thePlayer.prevRotationPitch && packet.rotation.y == thePlayer.prevRotationYaw) {
				return null
			} else {
				mode = MovePlayerPacket.Mode.HEAD_ROTATION
			}
		}

		return MovePlayerPacket().apply {
			runtimeEntityId = thePlayer.runtimeEntityId
			thePlayer.rideEntity?.also { ride -> ridingRuntimeEntityId = ride; println(ride) }
			this.mode = mode
			isOnGround = thePlayer.onGround
			tick = packet.tick
			rotation = packet.rotation
			position = packet.position
		}
	}

    companion object {
        const val RECOMMENDED_VERSION = "1.19.73.02"
		const val COLORED_NAME = "§9§lProtoHax§r"
    }
}
