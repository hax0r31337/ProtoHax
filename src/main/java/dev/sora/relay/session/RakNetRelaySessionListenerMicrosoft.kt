package dev.sora.relay.session

import coelho.msftauth.api.xbox.*
import com.google.gson.JsonParser
import com.nimbusds.jose.shaded.json.JSONObject
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.ClientToServerHandshakePacket
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket
import com.nukkitx.protocol.bedrock.util.EncryptionUtils
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.utils.CipherPair
import dev.sora.relay.utils.HttpUtils
import dev.sora.relay.utils.JoseStuff
import io.netty.util.AsciiString
import java.security.KeyPair
import java.security.Signature
import java.util.*

class RakNetRelaySessionListenerMicrosoft(val accessToken: String, private val session: RakNetRelaySession) : RakNetRelaySessionListener.PacketListener {

    private val keyPair = EncryptionUtils.createKeyPair()

    override fun onPacketInbound(packet: BedrockPacket): Boolean {
        if (packet is ServerToClientHandshakePacket) {
            val jwtSplit = packet.jwt.split(".")
            val headerObject = JsonParser.parseString(Base64.getDecoder().decode(jwtSplit[0]).toString(Charsets.UTF_8)).asJsonObject
            val payloadObject = JsonParser.parseString(Base64.getDecoder().decode(jwtSplit[1]).toString(Charsets.UTF_8)).asJsonObject
            val serverKey = EncryptionUtils.generateKey(headerObject.get("x5u").asString)
            val key = EncryptionUtils.getSecretKey(keyPair.private, serverKey,
                Base64.getDecoder().decode(payloadObject.get("salt").asString))
            session.serverCipher = CipherPair(key)
            session.outboundPacket(ClientToServerHandshakePacket())
            return false
        }

        return true
    }

    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
        if (packet is LoginPacket) {
            packet.chainData = AsciiString(fetchChain(accessToken))
            val skinBody = packet.skinData.toString().split(".")
            packet.skinData = AsciiString(toJWTRaw(skinBody[1], keyPair))
        }

        return true
    }

    private fun fetchChain(accessToken: String): String {
        val key = XboxDeviceKey() // this key used to sign the post content

        val userToken = XboxUserAuthRequest(
            "http://auth.xboxlive.com", "JWT", "RPS",
            "user.auth.xboxlive.com", "t=$accessToken"
        ).request()
        val deviceToken = XboxDeviceAuthRequest(
            "http://auth.xboxlive.com", "JWT", "Nintendo",
            "0.0.0.0", key
        ).request()
        val titleToken = XboxTitleAuthRequest(
            "http://auth.xboxlive.com", "JWT", "RPS",
            "user.auth.xboxlive.com", "t=$accessToken", deviceToken.token, key
        ).request()
        val xstsToken = XboxXSTSAuthRequest(
            "https://multiplayer.minecraft.net/",
            "JWT",
            "RETAIL",
            listOf(userToken),
            titleToken,
            XboxDevice(key, deviceToken)
        ).request()

        // use the xsts token to generate the identity token
        val identityToken = xstsToken.toIdentityToken()

        // then, we can request the chain
        val data = JSONObject().apply {
            put("identityPublicKey", Base64.getEncoder().encodeToString(keyPair.public.encoded))
        }
        println(data.toJSONString())
        val connection = HttpUtils.make("https://multiplayer.minecraft.net/authentication", "POST", data.toJSONString(),
            mapOf("Content-Type" to "application/json", "Authorization" to identityToken,
                "User-Agent" to "MCPE/UWP", "Client-Version" to "1.19.40"))

        return connection.inputStream.reader().readText()
    }

    private fun toJWTRaw(payload: String, keyPair: KeyPair): String {
        val headerJson = JSONObject().apply {
            put("alg", "ES384")
            put("x5u", Base64.getEncoder().encodeToString(keyPair.public.encoded))
        }
        println(headerJson.toJSONString())
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