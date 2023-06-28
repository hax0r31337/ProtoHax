package dev.sora.relay.session.listener.xbox.cache

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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

	override fun cache(device: XboxDeviceInfo, expires: Long, body: List<String>, keyPair: KeyPair) {
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
					it.add(jwt)
				}
			})
			addProperty("privateKey", Base64.getEncoder().withoutPadding().encodeToString(keyPair.private.encoded))
			addProperty("publicKey", Base64.getEncoder().withoutPadding().encodeToString(keyPair.public.encoded))
		})

		json.add(identifier, identifierJson)

		removeExpired(json)
		cacheFile.writeText(AbstractConfigManager.DEFAULT_GSON.toJson(json), Charsets.UTF_8)
	}

	override fun checkCache(device: XboxDeviceInfo): Pair<List<String>, KeyPair>? {
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
				removeExpired(json)
				cacheFile.writeText(AbstractConfigManager.DEFAULT_GSON.toJson(json), Charsets.UTF_8)
				return null
			}

			val publicKey = EncryptionUtils.parseKey(deviceJson.get("publicKey").asString)
			val privateKey = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(deviceJson.get("privateKey").asString)))

			val signedJwtList = mutableListOf<String>()
			deviceJson.getAsJsonArray("chain").forEach { jwt ->
				signedJwtList.add(jwt.asString)
			}
			return signedJwtList to KeyPair(publicKey, privateKey)
		} catch (e: Throwable) {
			logError("check cache", e)
			return null
		}
	}

	private fun removeExpired(json: JsonObject) {
		val toRemove = mutableListOf<String>()
		val epoch = Instant.now().epochSecond

		json.entrySet().forEach { (_, value) ->
			val identifierJson = value.asJsonObjectOrNull ?: return@forEach
			toRemove.clear()
			identifierJson.entrySet().forEach FE1@ { (key, value) ->
				val deviceJson = value.asJsonObjectOrNull ?: return@FE1
				if (deviceJson.get("expires").asLong < epoch) {
					toRemove.add(key)
				}
			}
			toRemove.forEach {
				identifierJson.remove(it)
			}
		}
	}
}
