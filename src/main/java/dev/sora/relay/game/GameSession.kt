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
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

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

    companion object {
        const val RECOMMENDED_VERSION = "1.19.50.02"
    }
}
