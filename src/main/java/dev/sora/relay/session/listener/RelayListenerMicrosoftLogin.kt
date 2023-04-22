package dev.sora.relay.session.listener

import coelho.msftauth.api.xbox.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jwt.SignedJWT
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.HttpUtils
import dev.sora.relay.utils.base64Decode
import dev.sora.relay.utils.logError
import dev.sora.relay.utils.logInfo
import org.cloudburstmc.protocol.bedrock.packet.*
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.io.InputStreamReader
import java.net.URI
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class RelayListenerMicrosoftLogin(val accessToken: String, val deviceInfo: DeviceInfo) : RelayListenerEncryptedSession() {

    constructor(accessToken: String, deviceInfo: DeviceInfo, session: MinecraftRelaySession) : this(accessToken, deviceInfo) {
        this.session = session
    }

    private var chainExpires = 0L
    private var identityToken = ""
        get() {
            if (field.isEmpty()) {
                field = fetchIdentityToken(accessToken, deviceInfo)
            }
            return field
        }
    private var chain: List<SignedJWT>? = null
        get() {
            if (field == null || chainExpires < Instant.now().epochSecond) {
                val chains = fetchChain(identityToken, keyPair)
				field = chains

				// search for chain expiry
				chainExpires = 0L
				chains.forEach {  chain ->
					val expires = chain.payload.toJSONObject()["exp"] ?: return@forEach
					if (expires is Number && (chainExpires == 0L || expires.toLong() < chainExpires)) {
						chainExpires = expires.toLong()
					}
				}
            }
            return field
        }

    fun forceFetchChain() {
        chain
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is LoginPacket) {
            try {
                packet.chain.clear()
                packet.chain.addAll(chain!!)
				packet.extra = signJWT(packet.extra.payload, keyPair)
            } catch (e: Throwable) {
                session.inboundPacket(DisconnectPacket().apply {
                    kickMessage = e.toString()
                })
                logError("login failed", e)
            }
            logInfo("login success")
        }

        return true
    }

    data class DeviceInfo(val appId: String, val deviceType: String,
                     val allowDirectTitleTokenFetch: Boolean = false)

    companion object {
        val DEVICE_ANDROID = DeviceInfo("0000000048183522", "Android", false)
        val DEVICE_NINTENDO = DeviceInfo("00000000441cc96b", "Nintendo", true)
        val devices = arrayOf(DEVICE_ANDROID, DEVICE_NINTENDO).associateBy { it.deviceType }

        fun fetchIdentityToken(accessToken: String, deviceInfo: DeviceInfo): String {
            val deviceKey = XboxDeviceKey() // this key used to sign the post content

            var userToken: XboxToken? = null
            val userRequestThread = thread {
                userToken = XboxUserAuthRequest(
                    "http://auth.xboxlive.com", "JWT", "RPS",
                    "user.auth.xboxlive.com", "t=$accessToken"
                ).request()
            }
            val deviceToken = XboxDeviceAuthRequest(
                "http://auth.xboxlive.com", "JWT", deviceInfo.deviceType,
                "0.0.0.0", deviceKey
            ).request()
            val titleToken = if (deviceInfo.allowDirectTitleTokenFetch) {
                XboxTitleAuthRequest(
                    "http://auth.xboxlive.com", "JWT", "RPS",
                    "user.auth.xboxlive.com", "t=$accessToken", deviceToken.token, deviceKey
                ).request()
            } else {
                val device = XboxDevice(deviceKey, deviceToken)
                val sisuRequest = XboxSISUAuthenticateRequest(
                    deviceInfo.appId, device, "service::user.auth.xboxlive.com::MBI_SSL",
                    XboxSISUAuthenticateRequest.Query("phone"),
                    "ms-xal-${deviceInfo.appId}://auth", "RETAIL"
                ).request()
                val sisuToken = try {
                    XboxSISUAuthorizeRequest(
                        "t=$accessToken", deviceInfo.appId, device, "RETAIL",
                        sisuRequest.sessionId, "user.auth.xboxlive.com", "http://xboxlive.com"
                    ).request()
                } catch (e: IllegalStateException) {
//                    val did = deviceToken.displayClaims["xdi"]!!.asJsonObject.get("did").asString
//                    val sign = deviceKey.sign("https://sisu.xboxlive.com/proxy", null, "POST", "sessionid=${sisuRequest.sessionId}".toByteArray()).replace("+", "%2B").replace("=","%3D")
//                    println("https://sisu.xboxlive.com/client/v28/0000000048183522/view/index.html?action=signup&redirect=ms-xal-0000000048183522://auth" +
//                            "&&did=$did&&sid=${sisuRequest.sessionId}&sig=${sign}")
                    throw RuntimeException("Have you registered a Xbox GamerTag?", e)
                }
                sisuToken.titleToken
            }
            if (userRequestThread.isAlive)
                userRequestThread.join()
            if (userToken == null) error("failed to fetch xbox user token")
            val xstsToken = XboxXSTSAuthRequest(
                "https://multiplayer.minecraft.net/",
                "JWT",
                "RETAIL",
                listOf(userToken),
                titleToken,
                XboxDevice(deviceKey, deviceToken)
            ).request()

            return xstsToken.toIdentityToken()
        }

        fun fetchRawChain(identityToken: String, publicKey: PublicKey): InputStreamReader {
            // then, we can request the chain
            val data = JsonObject().apply {
                addProperty("identityPublicKey", Base64.getEncoder().encodeToString(publicKey.encoded))
            }
            val connection = HttpUtils.make("https://multiplayer.minecraft.net/authentication", "POST", AbstractConfigManager.DEFAULT_GSON.toJson(data),
                mapOf("Content-Type" to "application/json", "Authorization" to identityToken,
                    "User-Agent" to "MCPE/UWP", "Client-Version" to "1.19.50"))

            return connection.inputStream.reader().let {
                val txt = it.readText()
                txt.toByteArray().inputStream().reader()
            }
        }

        fun fetchChain(identityToken: String, keyPair: KeyPair): List<SignedJWT> {
            val rawChain = JsonParser.parseReader(fetchRawChain(identityToken, keyPair.public)).asJsonObject
            val chains = rawChain.get("chain").asJsonArray

            // add the self-signed jwt
            val identityPubKey = JsonParser.parseString(base64Decode(chains.get(0).asString.split(".")[0]).toString(Charsets.UTF_8)).asJsonObject

            val jwt = signJWT(Payload(AbstractConfigManager.DEFAULT_GSON.toJson(JsonObject().apply {
				addProperty("certificateAuthority", true)
				addProperty("exp", (Instant.now().epochSecond + TimeUnit.HOURS.toSeconds(6)).toInt())
				addProperty("nbf", (Instant.now().epochSecond - TimeUnit.HOURS.toSeconds(6)).toInt())
				addProperty("identityPublicKey", identityPubKey.get("x5u").asString)
			})), keyPair)

            val list = mutableListOf(jwt)
			list.addAll(chains.map { SignedJWT.parse(it.asString) })
            return list
        }
    }
}
