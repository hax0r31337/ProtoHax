package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.utils.constants.Effect
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket

class ModuleFastBreak : CheatModule("FastBreak") {

    private var amplifierValue by intValue("Level", 5, 1..128)

	private val handleTick = handle<EventTick> { event ->
		if (event.session.thePlayer.tickExists % 20 != 0L) return@handle
		event.session.netSession.inboundPacket(MobEffectPacket().apply {
			setEvent(MobEffectPacket.Event.ADD)
			runtimeEntityId = event.session.thePlayer.runtimeEntityId
			effectId = Effect.HASTE
			amplifier = amplifierValue - 1
			isParticles = false
			duration = 21 * 20
		})
	}

    override fun onDisable() {
        if (!session.netSessionInitialized) return
        session.netSession.inboundPacket(MobEffectPacket().apply {
            runtimeEntityId = session.thePlayer.runtimeEntityId
            event = MobEffectPacket.Event.REMOVE
            effectId = Effect.HASTE
        })
    }
}
