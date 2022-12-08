package dev.sora.relay.game

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import com.nukkitx.protocol.bedrock.packet.StartGamePacket
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventManager
import dev.sora.relay.game.event.impl.EventPacketInbound
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.game.event.impl.EventTick
import dev.sora.relay.game.world.WorldClient

class GameSession : RakNetRelaySessionListener.PacketListener {

    val thePlayer = EntityPlayerSP()
    val theWorld = WorldClient(this)

    val eventManager = EventManager()

    lateinit var netSession: RakNetRelaySession

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        if (packet is StartGamePacket) {
            thePlayer.entityId = packet.runtimeEntityId
        }
        theWorld.onPacket(packet)

        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        val event = EventPacketOutbound(this, packet)
        eventManager.emit(event)
        if (event.isCanceled()) {
            return false
        }

        if (packet is MovePlayerPacket && packet.runtimeEntityId == thePlayer.entityId) {
            thePlayer.move(packet.position)
            thePlayer.rotate(packet.rotation)
            onTick()
        } else if (packet is PlayerAuthInputPacket) {
            thePlayer.move(packet.position)
            thePlayer.rotate(packet.rotation)
            onTick()
        }

        return true
    }

    fun onTick() {
        eventManager.emit(EventTick(this))
    }
}