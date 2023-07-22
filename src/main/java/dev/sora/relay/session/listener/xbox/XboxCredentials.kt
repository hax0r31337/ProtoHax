package dev.sora.relay.session.listener.xbox

import coelho.msftauth.api.xbox.XboxDevice
import coelho.msftauth.api.xbox.XboxToken
import coelho.msftauth.api.xbox.XboxXSTSAuthRequest
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.session.listener.xbox.cache.XboxIdentityToken
import dev.sora.relay.utils.HttpUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.util.*

class XboxCredentials(val userToken: XboxToken, val titleToken: XboxToken, val device: XboxDevice) {

	fun xstsAuth(relyingParty: String): XboxToken {
		return XboxXSTSAuthRequest(
			relyingParty,
			"JWT",
			"RETAIL",
			listOf(userToken),
			titleToken,
			device
		).request(HttpUtils.client)
	}

	fun fetchIdentityToken(): XboxIdentityToken {
		val token = xstsAuth("https://multiplayer.minecraft.net/")

		return XboxIdentityToken(token.toIdentityToken(), Instant.parse(token.notAfter).epochSecond)
	}

	fun fetchPlayFabTicket(): String {
		val token = xstsAuth("rp://playfabapi.com/").toIdentityToken()

		val data = JsonObject().apply {
			addProperty("CreateAccount", true)
			add("EncryptedRequest", JsonNull.INSTANCE)

			val infoRequestParameters = JsonObject().apply {
				addProperty("GetCharacterInventories", false)
				addProperty("GetCharacterList", false)
				addProperty("GetPlayerProfile", true)
				addProperty("GetPlayerStatistics", false)
				addProperty("GetTitleData", false)
				addProperty("GetUserAccountInfo", true)
				addProperty("GetUserData", false)
				addProperty("GetUserInventory", false)
				addProperty("GetUserReadOnlyData", false)
				addProperty("GetUserVirtualCurrency", false)
				add("PlayerStatisticNames", JsonNull.INSTANCE)
				add("ProfileConstraints", JsonNull.INSTANCE)
				add("TitleDataKeys", JsonNull.INSTANCE)
				add("UserDataKeys", JsonNull.INSTANCE)
				add("UserReadOnlyDataKeys", JsonNull.INSTANCE)
			}

			add("InfoRequestParameters", infoRequestParameters)
			add("PlayerSecret", JsonNull.INSTANCE)
			addProperty("TitleId", RelayListenerXboxLogin.MC_PLAYFAB_TITLE_ID)
			addProperty("XboxToken", token)
		}


		val request = Request.Builder()
			.url("https://20ca2.playfabapi.com/Client/LoginWithXbox?sdk=XPlatCppSdk-3.6.190304")
			.post(AbstractConfigManager.DEFAULT_GSON.toJson(data).toRequestBody("application/json".toMediaType()))
			.header("X-PlayFabSdk", "XPlatCppSdk-3.6.190304")
			.build()
		val response = HttpUtils.client.newCall(request).execute()

		if (response.code != 200) {
			throw IllegalStateException("PlayFabApi: ${response.code}")
		}

		val json = JsonParser.parseReader(response.body!!.charStream()).asJsonObject
		return json.getAsJsonObject("data").get("SessionTicket").asString
	}

	fun fetchMinecraftToken(): String {
		val playFabTicket = fetchPlayFabTicket()

		val data = JsonObject().apply {
			val device = JsonObject().apply {
				addProperty("applicationType", "MinecraftPE")
				add("capabilities", JsonNull.INSTANCE)
				addProperty("gameVersion", RelayListenerXboxLogin.MC_VERSION)
				addProperty("id", UUID.randomUUID().toString().replace("-", ""))
				addProperty("memory", "0")
				addProperty("platform", "Android")
				addProperty("playFabTitleId", RelayListenerXboxLogin.MC_PLAYFAB_TITLE_ID)
				addProperty("storePlatform", "android.googleplay")
				add("treatmentOverrides", JsonNull.INSTANCE)
				addProperty("type", "Android")
			}

			val user = JsonObject().apply {
				addProperty("language", "en")
				addProperty("languageCode", "en-US")
				addProperty("regionCode", "US")
				addProperty("token", playFabTicket)
				addProperty("tokenType", "PlayFab")
			}

			add("device", device)
			add("user", user)
		}

		val request = Request.Builder()
			.url("https://authorization.franchise.minecraft-services.net/api/v1.0/session/start")
			.post(AbstractConfigManager.DEFAULT_GSON.toJson(data).toRequestBody("application/json".toMediaType()))
			.build()
		val response = HttpUtils.client.newCall(request).execute()

		if (response.code != 200) {
			throw IllegalStateException("MinecraftFranchise: ${response.code}")
		}

		val json = JsonParser.parseReader(response.body!!.charStream()).asJsonObject
		return json.getAsJsonObject("result").get("authorizationHeader").asString
	}
}
