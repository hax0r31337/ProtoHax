package dev.sora.relay.session.listener

import com.google.gson.JsonParser
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jwt.SignedJWT
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.base64Decode
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.net.URI
import java.security.KeyPair
import java.security.interfaces.ECPrivateKey
import java.util.*

open class RelayListenerEncryptedSession() : MinecraftRelayPacketListener {

	constructor(session: MinecraftRelaySession) : this() {
		this.session = session
	}

	protected val keyPair = EncryptionUtils.createKeyPair()

	lateinit var session: MinecraftRelaySession


	open override fun onPacketInbound(packet: BedrockPacket): Boolean {
		if (packet is ServerToClientHandshakePacket) {
			val jwtSplit = packet.jwt.split(".")
			val headerObject = JsonParser.parseString(base64Decode(jwtSplit[0]).toString(Charsets.UTF_8)).asJsonObject
			val payloadObject = JsonParser.parseString(base64Decode(jwtSplit[1]).toString(Charsets.UTF_8)).asJsonObject
			val serverKey = EncryptionUtils.generateKey(headerObject.get("x5u").asString)
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
			var newChain: SignedJWT? = null
			packet.chain.forEach {
				val chainBody = JsonParser.parseString(it.payload.toString()).asJsonObject
				if (chainBody.has("extraData")) {
					chainBody.addProperty("identityPublicKey", Base64.getEncoder().encodeToString(keyPair.public.encoded))
					newChain = signJWT(Payload(AbstractConfigManager.DEFAULT_GSON.toJson(chainBody)), keyPair)
				}
			}
			packet.chain.clear()
			packet.chain.add(newChain)
		}

		return true
	}

	companion object {

		fun signJWT(payload: Payload, keyPair: KeyPair): SignedJWT {
			val header = JWSHeader.Builder(JWSAlgorithm.ES384)
				.x509CertURL(URI(Base64.getEncoder().encodeToString(keyPair.public.encoded)))
				.build()
			val jws = JWSObject(header, payload)
			EncryptionUtils.signJwt(jws, keyPair.private as ECPrivateKey)
			return SignedJWT(jws.header.toBase64URL(), jws.payload.toBase64URL(), jws.signature)
		}
	}
}
