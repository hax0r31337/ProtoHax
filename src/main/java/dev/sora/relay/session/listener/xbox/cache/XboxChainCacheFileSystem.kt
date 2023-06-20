package dev.sora.relay.session.listener.xbox.cache

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nimbusds.jwt.SignedJWT
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.session.listener.xbox.XboxDeviceInfo
import dev.sora.relay.utils.asJsonObjectOrNull
import dev.sora.relay.utils.logError
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*

class XboxChainCacheFileSystem(val cacheFile: File, override val identifier: String) : IXboxChainCache {

	override fun cache(device: XboxDeviceInfo, expires: Long, body: List<SignedJWT>, keyPair: KeyPair) {
		val json = if (!cacheFile.exists()) {
			null
		} else {
			try {
				JsonParser.parseReader(cacheFile.reader(Charsets.UTF_8)).asJsonObjectOrNull
			} catch (t: Throwable) {
				logError("load config", t)
				null
			}
		} ?: JsonObject()

		val identifierJson = if (json.has(identifier)) {
			val identifierElement = json.get(identifier)
			if (identifierElement.isJsonObject) {
				identifierElement.asJsonObject
			} else {
				JsonObject()
			}
		} else {
			JsonObject()
		}

		identifierJson.add(device.deviceType, JsonObject().apply {
			addProperty("expires", expires)
			add("chain", JsonArray().also {
				body.forEach { jwt ->
					it.add(jwt.serialize())
				}
			})
			addProperty("privateKey", Base64.getEncoder().encodeToString(keyPair.private.encoded))
			addProperty("publicKey", Base64.getEncoder().encodeToString(keyPair.public.encoded))
		})

		json.add(identifier, identifierJson)

		cacheFile.writeText(AbstractConfigManager.DEFAULT_GSON.toJson(json), Charsets.UTF_8)
	}

	override fun checkCache(device: XboxDeviceInfo): Pair<List<SignedJWT>, KeyPair>? {
		if (!cacheFile.exists()) {
			return null
		}

		val json = try {
			JsonParser.parseReader(cacheFile.reader(Charsets.UTF_8)).asJsonObjectOrNull
		} catch (t: Throwable) {
			logError("load config", t)
			null
		} ?: return null

		try {
			val identifierJson = if (json.has(identifier)) {
				json.get(identifier).asJsonObject
			} else {
				return null
			}

			val deviceJson = if (identifierJson.has(device.deviceType)) {
				identifierJson.get(device.deviceType).asJsonObject
			} else {
				return null
			}

			if (deviceJson.get("expires").asLong < Instant.now().epochSecond || !deviceJson.has("chain")
				|| !deviceJson.has("privateKey") || !deviceJson.has("publicKey")) {
				// remove cache due to cache expired or corrupted
				identifierJson.remove(device.deviceType)
				cacheFile.writeText(AbstractConfigManager.DEFAULT_GSON.toJson(json), Charsets.UTF_8)
				return null
			}

			val publicKey = EncryptionUtils.generateKey(deviceJson.get("publicKey").asString)
			val privateKey = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(deviceJson.get("privateKey").asString)))

			val signedJwtList = mutableListOf<SignedJWT>()
			deviceJson.getAsJsonArray("chain").forEach { jwt ->
				signedJwtList.add(SignedJWT.parse(jwt.asString))
			}
			return signedJwtList to KeyPair(publicKey, privateKey)
		} catch (e: Throwable) {
			logError("check cache", e)
			return null
		}
	}
}
