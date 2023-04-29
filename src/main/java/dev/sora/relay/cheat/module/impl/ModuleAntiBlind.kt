package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.utils.constants.Effect
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket

class ModuleAntiBlind : CheatModule("AntiBlind") {

    private var nightVisionValue by boolValue("NightVision", true)
    private var removeBadEffectsValue by boolValue("RemoveBadEffects", true)

	override fun onDisable() {
		if (nightVisionValue && session.netSessionInitialized) {
			session.netSession.inboundPacket(MobEffectPacket().apply {
				event = MobEffectPacket.Event.REMOVE
				runtimeEntityId = session.thePlayer.runtimeEntityId
				effectId = Effect.NIGHT_VISION
			})
		}
	}

	private val handleTick = handle<EventTick>(this::nightVisionValue) { event ->
		val session = event.session
		if (session.thePlayer.tickExists % 20 != 0L) return@handle
		session.netSession.inboundPacket(MobEffectPacket().apply {
			runtimeEntityId = session.thePlayer.runtimeEntityId
			setEvent(MobEffectPacket.Event.ADD)
			effectId = Effect.NIGHT_VISION
			amplifier = 0
			isParticles = false
			duration = 360000
		})
	}

	private val handlePacketInbound = handle<EventPacketInbound>(this::removeBadEffectsValue) { event ->
		val packet = event.packet
		if (packet is MobEffectPacket) {
			if (packet.effectId == Effect.NAUSEA || packet.effectId == Effect.BLINDNESS || packet.effectId == Effect.DARKNESS) {
				event.cancel()
			}
		}
	}
}
