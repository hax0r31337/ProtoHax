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
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket

class GameSession : MinecraftRelayPacketListener {

    val thePlayer = EntityPlayerSP(this)
    val theWorld = WorldClient(this)

    val cacheManager = BlobCacheManager()

    val eventManager = EventManager()

    lateinit var netSession: MinecraftRelaySession

    var blockMapping = BlockMapping(emptyMap(), 0)
        private set
    var legacyBlockMapping = LegacyBlockMapping(emptyMap())
        private set


    val netSessionInitialized: Boolean
        get() = this::netSession.isInitialized

    init {
        eventManager.registerListener(thePlayer)
        eventManager.registerListener(theWorld)
        eventManager.registerListener(cacheManager)
    }

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

    companion object {
        const val RECOMMENDED_VERSION = "1.19.50.02"
    }
}