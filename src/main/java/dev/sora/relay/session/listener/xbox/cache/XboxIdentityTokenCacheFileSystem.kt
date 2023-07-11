package dev.sora.relay.session.listener.xbox.cache

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.session.listener.xbox.XboxDeviceInfo
import dev.sora.relay.utils.asJsonObjectOrNull
import dev.sora.relay.utils.logError
import java.io.File
import java.time.Instant

class XboxIdentityTokenCacheFileSystem(val cacheFile: File, override val identifier: String) : IXboxIdentityTokenCache {

	override fun cache(device: XboxDeviceInfo, token: XboxIdentityToken) {
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
			addProperty("token", token.token)
			addProperty("expires", token.notAfter)
		})

		json.add(identifier, identifierJson)

		removeExpired(json)
		cacheFile.writeText(AbstractConfigManager.DEFAULT_GSON.toJson(json), Charsets.UTF_8)
	}

	override fun checkCache(device: XboxDeviceInfo): XboxIdentityToken? {
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

			if (deviceJson.get("expires").asLong < Instant.now().epochSecond || !deviceJson.has("token")) {
				// remove cache due to cache expired or corrupted
				identifierJson.remove(device.deviceType)
				removeExpired(json)
				cacheFile.writeText(AbstractConfigManager.DEFAULT_GSON.toJson(json), Charsets.UTF_8)
				return null
			}

			return XboxIdentityToken(deviceJson.get("token").asString, deviceJson.get("expires").asLong)
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
