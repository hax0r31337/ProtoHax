package dev.sora.relay.session.listener

import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.jwtPayload
import dev.sora.relay.utils.signJWT
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.util.*

open class RelayListenerEncryptedSession() : MinecraftRelayPacketListener {

	constructor(session: MinecraftRelaySession) : this() {
		this.session = session
	}

	protected var keyPair = EncryptionUtils.createKeyPair()

	lateinit var session: MinecraftRelaySession

	open override fun onPacketOutbound(packet: BedrockPacket): Boolean {
		if (packet is LoginPacket) {
			session.keyPair = keyPair
			// only extraData required for offline mode login
			var newChain: String? = null
			packet.chain.forEach {
				val chainBody = jwtPayload(it) ?: return@forEach
				if (chainBody.has("extraData")) {
					chainBody.addProperty("identityPublicKey", Base64.getEncoder().withoutPadding().encodeToString(keyPair.public.encoded))
					newChain = signJWT(AbstractConfigManager.DEFAULT_GSON.toJson(chainBody), keyPair)
				}
			}
			packet.chain.clear()
			packet.chain.add(newChain)
		}

		return true
	}
}
