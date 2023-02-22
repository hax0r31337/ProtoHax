package dev.sora.relay.session.listener

import coelho.msftauth.api.xbox.*
import com.google.gson.JsonParser
import com.nimbusds.jose.shaded.json.JSONObject
import com.nimbusds.jwt.SignedJWT
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.*
import org.cloudburstmc.protocol.bedrock.packet.*
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.io.InputStreamReader
import java.security.KeyPair
import java.security.PublicKey
import java.security.Signature
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class RelayListenerMicrosoftLogin(val accessToken: String, val deviceInfo: DeviceInfo) : MinecraftRelayPacketListener {

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
    private var chain: List<String>? = null
        get() {
            if (field == null || chainExpires < Instant.now().epochSecond) {
                field = fetchChain(identityToken, keyPair).also {
                    val json = JsonParser.parseReader(base64Decode(it[0].split(".")[1])
                        .inputStream().reader()).asJsonObject
                    chainExpires = json.get("exp").asLong
                }
            }
            return field
        }

    private val keyPair = EncryptionUtils.createKeyPair()

    lateinit var session: MinecraftRelaySession

    fun forceFetchChain() {
        chain
    }

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
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

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is LoginPacket) {
            try {
                packet.chain.clear()
                chain!!.forEach {
                    packet.chain.add(SignedJWT.parse(it))
                }
                packet.extra = SignedJWT.parse(toJWTRaw(packet.extra.payload.toBase64URL().toString(), keyPair))
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
            val data = JSONObject().apply {
                put("identityPublicKey", Base64.getEncoder().encodeToString(publicKey.encoded))
            }
            val connection = HttpUtils.make("https://multiplayer.minecraft.net/authentication", "POST", data.toJSONString(),
                mapOf("Content-Type" to "application/json", "Authorization" to identityToken,
                    "User-Agent" to "MCPE/UWP", "Client-Version" to "1.19.50"))

            return connection.inputStream.reader().let {
                val txt = it.readText()
                txt.toByteArray().inputStream().reader()
            }
        }

        fun fetchChain(identityToken: String, keyPair: KeyPair): List<String> {
            val rawChain = JsonParser.parseReader(fetchRawChain(identityToken, keyPair.public)).asJsonObject
            val chains = rawChain.get("chain").asJsonArray

            // add the self-signed jwt
            val identityPubKey = JsonParser.parseString(base64Decode(chains.get(0).asString.split(".")[0]).toString(Charsets.UTF_8)).asJsonObject
            val jwt = toJWTRaw(Base64.getEncoder().encodeToString(JSONObject().apply {
                put("certificateAuthority", true)
                put("exp", (Instant.now().epochSecond + TimeUnit.HOURS.toSeconds(6)).toInt())
                put("nbf", (Instant.now().epochSecond - TimeUnit.HOURS.toSeconds(6)).toInt())
                put("identityPublicKey", identityPubKey.get("x5u").asString)
            }.toJSONString().toByteArray(Charsets.UTF_8)), keyPair)

            val list = mutableListOf<String>(jwt)
            chains.forEach {
                list.add(it.asString)
            }
            return list
        }

        private fun toJWTRaw(payload: String, keyPair: KeyPair): String {
            val headerJson = JSONObject().apply {
                put("alg", "ES384")
                put("x5u", Base64.getEncoder().encodeToString(keyPair.public.encoded))
            }
            val header = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.toJSONString().toByteArray(Charsets.UTF_8))
            val sign = signBytes("$header.$payload".toByteArray(Charsets.UTF_8), keyPair)
            return "$header.$payload.$sign"
        }

        private fun signBytes(dataToSign: ByteArray, keyPair: KeyPair): String {
            val signature = Signature.getInstance("SHA384withECDSA")
            signature.initSign(keyPair.private)
            signature.update(dataToSign)
            val signatureBytes = JoseStuff.DERToJOSE(signature.sign())
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes)
        }
    }
}