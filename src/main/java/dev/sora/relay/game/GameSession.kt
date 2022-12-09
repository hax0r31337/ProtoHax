package dev.sora.relay.game

import com.google.gson.JsonParser
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.DisconnectPacket
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import com.nukkitx.protocol.bedrock.packet.RespawnPacket
import com.nukkitx.protocol.bedrock.packet.StartGamePacket
import com.nukkitx.protocol.bedrock.packet.TransferPacket
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.cheat.BasicThing
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventManager
import dev.sora.relay.game.event.impl.EventPacketInbound
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.game.event.impl.EventTick
import dev.sora.relay.game.world.WorldClient
import java.util.Base64
import java.util.UUID

class GameSession : RakNetRelaySessionListener.PacketListener {

    val thePlayer = EntityPlayerSP()
    val theWorld = WorldClient(this)

    val eventManager = EventManager()

    lateinit var netSession: RakNetRelaySession

    var xuid = ""
    var identity = UUID.randomUUID().toString()
    var displayName = "Player"

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        if (packet is StartGamePacket && thePlayer.entityId == 0L) {
            thePlayer.entityId = packet.runtimeEntityId
        } else if (packet is RespawnPacket) {
            thePlayer.entityId = packet.runtimeEntityId
        }
        thePlayer.onPacket(packet)
        theWorld.onPacket(packet)

        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        val event = EventPacketOutbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        if (packet is MovePlayerPacket) {
            thePlayer.move(packet.position)
            thePlayer.rotate(packet.rotation)
            if (packet.runtimeEntityId != thePlayer.entityId) {
                thePlayer.entityId = packet.runtimeEntityId
                BasicThing.chat(this, "runtimeEntityId mismatch, desync occur? (client=${packet.runtimeEntityId}, relay=${thePlayer.entityId})")
            }
            onTick()
        } else if (packet is PlayerAuthInputPacket) {
            thePlayer.move(packet.position)
            thePlayer.rotate(packet.rotation)
            onTick()
        } else if (packet is LoginPacket) {
            val body = JsonParser.parseString(packet.chainData.toString()).asJsonObject.getAsJsonArray("chain")
            for (chain in body) {
                val chainBody = JsonParser.parseString(Base64.getDecoder().decode(chain.asString.split(".")[1]).toString(Charsets.UTF_8)).asJsonObject
                if (chainBody.has("extraData")) {
                    val xData = chainBody.getAsJsonObject("extraData")
                    xuid = xData.get("XUID").asString
                    identity = xData.get("identity").asString
                    displayName = xData.get("displayName").asString
                }
            }
        }

        return true
    }

    fun onTick() {
        eventManager.emit(EventTick(this))
    }
}