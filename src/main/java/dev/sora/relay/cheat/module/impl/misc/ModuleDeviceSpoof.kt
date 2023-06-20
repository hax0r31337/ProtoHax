package dev.sora.relay.cheat.module.impl.misc

import com.google.gson.JsonParser
import com.nimbusds.jose.Payload
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.utils.constants.DeviceOS
import dev.sora.relay.session.listener.RelayListenerEncryptedSession
import dev.sora.relay.utils.toHexString
import org.cloudburstmc.protocol.bedrock.data.InputMode
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import kotlin.random.Random

class ModuleDeviceSpoof : CheatModule("DeviceSpoof", CheatCategory.MISC) {

    private var deviceIdValue by boolValue("DeviceId", true)
    private var platformValue by boolValue("Platform", true)

	private val handlePacketOutbound = handle<EventPacketOutbound> {
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

			packet.extra = RelayListenerEncryptedSession.signJWT(Payload(AbstractConfigManager.DEFAULT_GSON.toJson(body)), EncryptionUtils.createKeyPair())
		} else if (platformValue && packet is PlayerAuthInputPacket) {
			packet.inputMode = InputMode.TOUCH
		}
	}
}
