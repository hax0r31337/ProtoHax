package dev.sora.relay.session

import coelho.msftauth.api.xbox.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.nimbusds.jose.shaded.json.JSONObject
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.ClientToServerHandshakePacket
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket
import com.nukkitx.protocol.bedrock.util.EncryptionUtils
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.utils.*
import io.netty.util.AsciiString
import java.io.InputStreamReader
import java.security.KeyPair
import java.security.Signature
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class RakNetRelaySessionListenerMicrosoft(val accessToken: String, val deviceInfo: DeviceInfo)
    : RakNetRelaySessionListener.PacketListener {

    constructor(accessToken: String, deviceInfo: DeviceInfo, session: RakNetRelaySession) : this(accessToken, deviceInfo) {
        this.session = session
    }

    private var chainExpires = 0L
    private var identityToken: String? = null
    private var chain: AsciiString? = null
        get() {
            if (field == null || chainExpires < Instant.now().epochSecond) {
                field = AsciiString(getChain(accessToken).also {
                    val json = JsonParser.parseReader(base64Decode(
                        JsonParser.parseString(it).asJsonObject.getAsJsonArray("chain").get(0).asString.split(".")[1])
                        .inputStream().reader()).asJsonObject
                    chainExpires = json.get("exp").asLong
                })
            }
            return field
        }

    private val keyPair = EncryptionUtils.createKeyPair()

    lateinit var session: RakNetRelaySession

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
            session.serverCipher = CipherPair(key)
            session.outboundPacket(ClientToServerHandshakePacket())
            return false
        }

        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is LoginPacket) {
            packet.chainData = AsciiString(chain)
            val skinBody = packet.skinData.toString().split(".")[1]
            packet.skinData = AsciiString(toJWTRaw(skinBody, keyPair))
            logInfo("login success")
        }

        return true
    }

    private fun getChain(accessToken: String): String {
        val rawChain = JsonParser.parseReader(fetchChain(accessToken)).asJsonObject
        val chains = rawChain.get("chain").asJsonArray

        // add the self-signed jwt
        val identityPubKey = JsonParser.parseString(base64Decode(chains.get(0).asString.split(".")[0]).toString(Charsets.UTF_8)).asJsonObject
        val jwt = toJWTRaw(Base64.getEncoder().encodeToString(JSONObject().apply {
            put("certificateAuthority", true)
            put("exp", (Instant.now().epochSecond + TimeUnit.HOURS.toSeconds(6)).toInt())
            put("nbf", (Instant.now().epochSecond - TimeUnit.HOURS.toSeconds(6)).toInt())
            put("identityPublicKey", identityPubKey.get("x5u").asString)
        }.toJSONString().toByteArray(Charsets.UTF_8)), keyPair)

        rawChain.add("chain", JsonArray().also {
            it.add(jwt)
            it.addAll(chains)
        })

        return Gson().toJson(rawChain)
    }

    private fun fetchIdentityToken(accessToken: String): String {
        if (identityToken == null) {

            val deviceKey = XboxDeviceKey() // this key used to sign the post content

            val userToken = XboxUserAuthRequest(
                "http://auth.xboxlive.com", "JWT", "RPS",
                "user.auth.xboxlive.com", "t=$accessToken"
            ).request()
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
                val sisuToken = XboxSISUAuthorizeRequest(
                    "t=$accessToken", deviceInfo.appId, device, "RETAIL",
                    sisuRequest.sessionId, "user.auth.xboxlive.com", "http://xboxlive.com"
                ).request()
                sisuToken.titleToken
            }
            val xstsToken = XboxXSTSAuthRequest(
                "https://multiplayer.minecraft.net/",
                "JWT",
                "RETAIL",
                listOf(userToken),
                titleToken,
                XboxDevice(deviceKey, deviceToken)
            ).request()

            // use the xsts token to generate the identity token
            identityToken = xstsToken.toIdentityToken()
        }
        return identityToken!!
    }

    private fun fetchChain(accessToken: String): InputStreamReader {
        val identityToken = fetchIdentityToken(accessToken)

        // then, we can request the chain
        val data = JSONObject().apply {
            put("identityPublicKey", Base64.getEncoder().encodeToString(keyPair.public.encoded))
        }
        val connection = HttpUtils.make("https://multiplayer.minecraft.net/authentication", "POST", data.toJSONString(),
            mapOf("Content-Type" to "application/json", "Authorization" to identityToken,
                "User-Agent" to "MCPE/UWP", "Client-Version" to "1.19.50"))

        return connection.inputStream.reader().let {
            val txt = it.readText()
            println(txt)
            txt.toByteArray().inputStream().reader()
        }
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

    data class DeviceInfo(val appId: String, val deviceType: String,
                     val allowDirectTitleTokenFetch: Boolean = false)

    companion object {
        val DEVICE_ANDROID = DeviceInfo("0000000048183522", "Android", false)
        val DEVICE_NINTENDO = DeviceInfo("00000000441cc96b", "Nintendo", true)
    }
}