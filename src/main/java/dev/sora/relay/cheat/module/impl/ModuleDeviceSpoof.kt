package dev.sora.relay.cheat.module.impl

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.nukkitx.protocol.bedrock.packet.LoginPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventPacketOutbound
import dev.sora.relay.utils.base64Decode
import dev.sora.relay.utils.getRandomString
import dev.sora.relay.utils.toHexString
import io.netty.util.AsciiString
import java.util.*
import kotlin.random.Random

class ModuleDeviceSpoof : CheatModule("DeviceSpoof") {

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet

        if (packet is LoginPacket) {
            val body = JsonParser.parseString(base64Decode(packet.skinData.toString().split(".")[1]).toString(Charsets.UTF_8)).asJsonObject
            body.addProperty("ClientRandomId", Random.nextLong())
            body.addProperty("DeviceModel", getRandomString(5 + Random.nextInt(5)))
            body.addProperty("DeviceId", Random.nextBytes(ByteArray(16)).toHexString())
            packet.skinData = AsciiString(".${Base64.getEncoder().encodeToString(Gson().toJson(body).toByteArray(Charsets.UTF_8))}.")
        }
    }
}