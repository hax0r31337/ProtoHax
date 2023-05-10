package dev.sora.relay.session.listener.xbox

import com.google.gson.JsonParser
import dev.sora.relay.utils.HttpUtils
import okhttp3.FormBody
import okhttp3.Request


data class XboxDeviceInfo(val appId: String, val deviceType: String,
						  val allowDirectTitleTokenFetch: Boolean = false,
						  val xalRedirect: String = "") {

	/**
	 * @param token refresh token or authorization code
	 * @return Pair<AccessToken, RefreshToken>
	 */
	fun refreshToken(token: String): Pair<String, String> {
		val form = FormBody.Builder()
		form.add("client_id", appId)
		form.add("redirect_uri", "https://login.live.com/oauth20_desktop.srf")
		// if the last part of the token was uuid, it must be authorization code
		if (token.split("\n")[0].substring(token.lastIndexOf('.')+1).length == 36) {
			form.add("grant_type", "authorization_code")
			form.add("code", token)
		} else {
			form.add("scope", "service::user.auth.xboxlive.com::MBI_SSL")
			form.add("grant_type", "refresh_token")
			form.add("refresh_token", token)
		}
		val request = Request.Builder()
			.url("https://login.live.com/oauth20_token.srf")
			.header("Content-Type", "application/x-www-form-urlencoded")
			.post(form.build())
			.build()
		val response = HttpUtils.client.newCall(request).execute()

		assert(response.code == 200) { "Http code ${response.code}" }

		val body = JsonParser.parseReader(response.body!!.charStream()).asJsonObject
		if (!body.has("access_token") || !body.has("refresh_token")) {
			if (body.has("error")) {
				throw RuntimeException("error occur whilst refreshing token: " + body.get("error").asString)
			} else {
				throw RuntimeException("error occur whilst refreshing token")
			}
		}
		return body.get("access_token").asString to body.get("refresh_token").asString
	}

	companion object {

		val DEVICE_ANDROID = XboxDeviceInfo("0000000048183522", "Android", false, xalRedirect = "ms-xal-0000000048183522://auth")
		val DEVICE_IOS = XboxDeviceInfo("000000004c17c01a", "iOS", false, xalRedirect = "ms-xal-000000004c17c01a://auth")
		val DEVICE_NINTENDO = XboxDeviceInfo("00000000441cc96b", "Nintendo", true)
		val devices = arrayOf(DEVICE_ANDROID, DEVICE_IOS, DEVICE_NINTENDO).associateBy { it.deviceType }
	}
}
