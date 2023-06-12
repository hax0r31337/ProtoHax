package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketOutbound
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

class ModuleBlink : CheatModule("Blink", CheatCategory.MOVEMENT) {

    private val packetList = mutableListOf<BedrockPacket>()

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		packetList.add(event.packet)
		event.cancel()
	}

    override fun onDisable() {
        for (packet in packetList) {
            session.netSession.outboundPacket(packet)
        }
        packetList.clear()
    }
}
