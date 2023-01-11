package dev.sora.relay.game

import com.google.gson.JsonParser
import com.nukkitx.network.util.DisconnectReason
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.RespawnPacket
import com.nukkitx.protocol.bedrock.packet.StartGamePacket
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.Event.EventManager
import dev.sora.relay.game.event.EventDisconnect
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.utils.mapping.BlockMappingUtils
import dev.sora.relay.game.utils.mapping.EmptyRuntimeMapping
import dev.sora.relay.game.utils.mapping.RuntimeMapping
import dev.sora.relay.game.world.WorldClient
import dev.sora.relay.utils.base64Decode
import java.util.*

class GameSession : RakNetRelaySessionListener.PacketListener {

    val thePlayer = EntityPlayerSP(this)
    val theWorld = WorldClient(this)

    val eventManager = EventManager()

    lateinit var netSession: RakNetRelaySession

    var xuid = ""
        private set
    var identity = UUID.randomUUID().toString()
        private set
    var displayName = "Player"
        private set

    var blockMapping: RuntimeMapping = EmptyRuntimeMapping()
    var legacyBlockMapping: RuntimeMapping = EmptyRuntimeMapping()

    val netSessionInitialized: Boolean
        get() = this::netSession.isInitialized

    init {
        eventManager.registerListener(thePlayer)
        eventManager.registerListener(theWorld)
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
            val body = JsonParser.parseString(packet.chainData.toString()).asJsonObject.getAsJsonArray("chain")
            for (chain in body) {
                val chainBody = JsonParser.parseString(base64Decode(chain.asString.split(".")[1]).toString(Charsets.UTF_8)).asJsonObject
                if (chainBody.has("extraData")) {
                    val xData = chainBody.getAsJsonObject("extraData")
                    xuid = xData.get("XUID").asString
                    identity = xData.get("identity").asString
                    displayName = xData.get("displayName").asString
                }
            }
            blockMapping = BlockMappingUtils.craftMapping(packet.protocolVersion)
            legacyBlockMapping = BlockMappingUtils.craftMapping(packet.protocolVersion, "legacy")
        }

        return true
    }

    override fun onDisconnect(client: Boolean, reason: DisconnectReason) {
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
        netSession.outboundPacket(packet)
    }
}