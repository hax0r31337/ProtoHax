package dev.sora.relay.cheat.module.impl.misc

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.entity.data.Effect
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket

class ModuleFastBreak : CheatModule("FastBreak", CheatCategory.MISC) {

    private var amplifierValue by intValue("Level", 5, 1..128)

	private val handleTick = handle<EventTick> {
		if (session.player.tickExists % 20 != 0L) return@handle
		session.netSession.inboundPacket(MobEffectPacket().apply {
			setEvent(MobEffectPacket.Event.ADD)
			runtimeEntityId = session.player.runtimeEntityId
			effectId = Effect.HASTE
			amplifier = amplifierValue - 1
			isParticles = false
			duration = 21 * 20
		})
	}

    override fun onDisable() {
        if (!session.netSessionInitialized) return
        session.netSession.inboundPacket(MobEffectPacket().apply {
            runtimeEntityId = session.player.runtimeEntityId
            event = MobEffectPacket.Event.REMOVE
            effectId = Effect.HASTE
        })
    }
}
