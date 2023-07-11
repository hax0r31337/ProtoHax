package dev.sora.relay.cheat.module.impl.visual

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.entity.data.Effect
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventTick
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket

class ModuleAntiBlind : CheatModule("AntiBlind", CheatCategory.VISUAL) {

    private var nightVisionValue by boolValue("NightVision", true)
	private var removeFireValue by boolValue("RemoveFire", false)
    private var removeBadEffectsValue by boolValue("RemoveBadEffects", true)
	private var removeInvisibleValue by boolValue("RemoveInvisible", true)

	override fun onDisable() {
		if (nightVisionValue && session.netSessionInitialized) {
			session.netSession.inboundPacket(MobEffectPacket().apply {
				event = MobEffectPacket.Event.REMOVE
				runtimeEntityId = session.player.runtimeEntityId
				effectId = Effect.NIGHT_VISION
			})
		}
	}

	private val handleTick = handle<EventTick>(this::nightVisionValue) {
		if (session.player.tickExists % 20 != 0L) return@handle
		session.netSession.inboundPacket(MobEffectPacket().apply {
			runtimeEntityId = session.player.runtimeEntityId
			setEvent(MobEffectPacket.Event.ADD)
			effectId = Effect.NIGHT_VISION
			amplifier = 0
			isParticles = false
			duration = 360000
		})
	}

	private val handlePacketInbound = handle<EventPacketInbound> {
		if (removeBadEffectsValue && packet is MobEffectPacket) {
			if (packet.effectId == Effect.NAUSEA || packet.effectId == Effect.BLINDNESS || packet.effectId == Effect.DARKNESS) {
				cancel()
			}
		} else if (packet is SetEntityDataPacket) {
			if (packet.runtimeEntityId == session.player.runtimeEntityId && packet.metadata?.flags != null) {
				if (removeFireValue && packet.metadata.flags.contains(EntityFlag.ON_FIRE)) {
					packet.metadata.setFlag(EntityFlag.ON_FIRE, false)
				}
			} else {
				if (removeInvisibleValue) {
					processInvisibleEntityData(packet.metadata)
				}
			}
		} else if (removeInvisibleValue && packet is AddEntityPacket) {
			processInvisibleEntityData(packet.metadata)
		} else if (removeInvisibleValue && packet is AddPlayerPacket) {
			processInvisibleEntityData(packet.metadata)
		}
	}

	private fun processInvisibleEntityData(metadata: EntityDataMap?) {
		metadata?.flags ?: return

		if (metadata.flags.contains(EntityFlag.INVISIBLE)) {
			metadata.setFlag(EntityFlag.INVISIBLE, false)
		}
	}
}
