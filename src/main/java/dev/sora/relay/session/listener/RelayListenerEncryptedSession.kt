package dev.sora.relay.session.listener

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.base64Decode
import dev.sora.relay.utils.jwtPayload
import dev.sora.relay.utils.signJWT
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.net.URI
import java.security.KeyPair
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.util.*

open class RelayListenerEncryptedSession() : MinecraftRelayPacketListener {

	constructor(session: MinecraftRelaySession) : this() {
		this.session = session
	}

	protected var keyPair = EncryptionUtils.createKeyPair()

	lateinit var session: MinecraftRelaySession

	open override fun onPacketInbound(packet: BedrockPacket): Boolean {
		if (packet is ServerToClientHandshakePacket) {
			val jwtSplit = packet.jwt.split(".")
			val headerObject = JsonParser.parseString(base64Decode(jwtSplit[0]).toString(Charsets.UTF_8)).asJsonObject
			val payloadObject = JsonParser.parseString(base64Decode(jwtSplit[1]).toString(Charsets.UTF_8)).asJsonObject
			val serverKey = EncryptionUtils.parseKey(headerObject.get("x5u").asString)
			val key = EncryptionUtils.getSecretKey(keyPair.private, serverKey,
				base64Decode(payloadObject.get("salt").asString))
			session.client!!.enableEncryption(key)
			session.outboundPacket(ClientToServerHandshakePacket())
			return false
		}

		return true
	}

	open override fun onPacketOutbound(packet: BedrockPacket): Boolean {
		if (packet is LoginPacket) {
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
