package dev.sora.relay.cheat.module.impl

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jwt.SignedJWT
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.utils.constants.DeviceOS
import dev.sora.relay.utils.toHexString
import org.cloudburstmc.protocol.bedrock.data.InputMode
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.security.interfaces.ECPrivateKey
import kotlin.random.Random

class ModuleDeviceSpoof : CheatModule("DeviceSpoof") {

    private var deviceIdValue by boolValue("DeviceId", true)
    private var platformValue by boolValue("Platform", true)

	private val handlePacketOutbound = handle<EventPacketOutbound> { event ->
		val packet = event.packet

		if (packet is LoginPacket) {
			val body = JsonParser.parseString(packet.extra.payload.toString()).asJsonObject
			if (deviceIdValue) {
				body.addProperty("ClientRandomId", Random.nextLong())
				body.addProperty("DeviceId", Random.nextBytes(ByteArray(16)).toHexString())
			}
			if (platformValue) {
				body.addProperty("DeviceModel","Nintendo Switch")
				body.addProperty("DeviceOS", DeviceOS.NINTENDO)
				body.addProperty("CurrentInputMode", 2) // Touch
			}

			val header = JWSHeader.Builder(JWSAlgorithm.ES384)
				.build()
			val jws = JWSObject(header, Payload(Gson().toJson(body)))
			EncryptionUtils.signJwt(jws, EncryptionUtils.createKeyPair().private as ECPrivateKey)

			packet.extra = SignedJWT.parse(jws.serialize())
		} else if (platformValue && packet is PlayerAuthInputPacket) {
			packet.inputMode = InputMode.TOUCH
		}
	}
}
