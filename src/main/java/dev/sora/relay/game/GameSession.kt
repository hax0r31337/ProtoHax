package dev.sora.relay.game

import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.*
import dev.sora.relay.game.management.BlobCacheManager
import dev.sora.relay.game.registry.BlockMapping
import dev.sora.relay.game.registry.ItemMapping
import dev.sora.relay.game.registry.LegacyBlockMapping
import dev.sora.relay.game.utils.constants.EnumFacing
import dev.sora.relay.game.world.WorldClient
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.logInfo
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.*

class GameSession : MinecraftRelayPacketListener {

	val eventManager = EventManager()

    val thePlayer = EntityPlayerSP(this, eventManager)
    val theWorld = WorldClient(this, eventManager)

    val cacheManager = BlobCacheManager(eventManager)

    lateinit var netSession: MinecraftRelaySession

    var blockMapping = BlockMapping(emptyMap(), 0)
        private set
    var legacyBlockMapping = LegacyBlockMapping(emptyMap())
        private set

    val netSessionInitialized: Boolean
        get() = this::netSession.isInitialized

	private var lastStopBreak = false

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
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
            val itemDefinitions = ItemMapping.Provider.craftMapping(protocolVersion)
            val blockDefinitions = BlockMapping.Provider.craftMapping(protocolVersion)
            netSession.peer.codecHelper.itemDefinitions = itemDefinitions
            netSession.peer.codecHelper.blockDefinitions = blockDefinitions
            netSession.client?.let {
                it.peer.codecHelper.itemDefinitions = itemDefinitions
                it.peer.codecHelper.blockDefinitions = blockDefinitions
            }
            blockMapping = blockDefinitions
            legacyBlockMapping = LegacyBlockMapping.Provider.craftMapping(packet.protocolVersion)
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
			message = "[§9§lProtoHax§r] $msg"
			xuid = ""
			sourceName = ""
		})
	}

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
    }
}
