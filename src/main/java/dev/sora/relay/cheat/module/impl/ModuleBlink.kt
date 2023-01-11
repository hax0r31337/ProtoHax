package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.BedrockPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Event.Listen
import dev.sora.relay.game.event.EventPacketOutbound

class ModuleBlink : CheatModule("Blink") {
    private val packetList = mutableListOf<BedrockPacket>()

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound){
        packetList.add(event.packet)
        event.cancel()
    }

    override fun onDisable(){
        for(packet in packetList){
            session.netSession.outboundPacket(packet)
        }
        packetList.clear()
    }
}