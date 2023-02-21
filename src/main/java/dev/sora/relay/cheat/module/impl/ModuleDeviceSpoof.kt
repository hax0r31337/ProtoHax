package dev.sora.relay.cheat.module.impl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.cloudburstmc.protocol.bedrock.data.InputMode
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.utils.constants.DeviceOS
import dev.sora.relay.utils.toHexString
import io.netty.util.AsciiString
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.security.interfaces.ECPrivateKey
import java.util.*
import kotlin.random.Random

class ModuleDeviceSpoof : CheatModule("DeviceSpoof") {

    private val deviceIdValue = boolValue("DeviceId", true)
    private val platformValue = boolValue("Platform", true)

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet

        if (packet is LoginPacket) {
            val body = JsonParser.parseString(packet.extra.payload.toString()).asJsonObject
            if (deviceIdValue.get()) {
                body.addProperty("ClientRandomId", Random.nextLong())
                body.addProperty("DeviceId", Random.nextBytes(ByteArray(16)).toHexString())
            }
            if (platformValue.get()) {
                body.addProperty("DeviceModel","Nintendo Switch")
                body.addProperty("DeviceOS", DeviceOS.NINTENDO)
                body.addProperty("CurrentInputMode", 2) // Touch
            }

            val header = JWSHeader.Builder(JWSAlgorithm.ES384)
                .build()
            val jws = JWSObject(header, Payload(Gson().toJson(body)))
            EncryptionUtils.signJwt(jws, EncryptionUtils.createKeyPair().private as ECPrivateKey)

            packet.extra = SignedJWT.parse(jws.serialize())
        } else if (platformValue.get() && packet is PlayerAuthInputPacket) {
            packet.inputMode = InputMode.TOUCH
        }
    }
}