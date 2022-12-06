package dev.sora.relay

import com.google.gson.JsonParser
import com.nukkitx.network.raknet.*
import com.nukkitx.network.util.DisconnectReason
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.Ability
import com.nukkitx.protocol.bedrock.packet.*
import com.nukkitx.protocol.bedrock.util.EncryptionUtils
import com.nukkitx.protocol.bedrock.v557.Bedrock_v557
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializers
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.HttpUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.util.internal.logging.InternalLoggerFactory
import java.io.File
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.*
import java.util.zip.Deflater
import javax.crypto.spec.SecretKeySpec


fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())

    val relay = RakNetRelay(InetSocketAddress("0.0.0.0", 19132), packetCodec = Bedrock_v557.V557_CODEC)
    relay.listener = object : RakNetRelayListener {
        override fun onQuery(address: InetSocketAddress) =
            "MCPE;RakNet Relay;557;1.19.20;0;10;${relay.server.guid};Bedrock level;Survival;1;19132;19132;".toByteArray()

        override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
            return InetSocketAddress("127.0.0.1", 19136)
//            return InetSocketAddress("101.67.57.190", 19114)
        }

        override fun onSession(session: RakNetRelaySession) {
            println("SESSION")
            var entityId = 0L
            session.listener.childListener.add(object : RakNetRelaySessionListener.PacketListener {
                override fun onPacketInbound(packet: BedrockPacket): Boolean {
                    if (packet !is LevelChunkPacket && packet !is UpdateBlockPacket) {
                        println(packet)
                    }
//                    if (packet is StartGamePacket) {
//                        entityId = packet.runtimeEntityId
//                    } else if (packet is UpdateAbilitiesPacket) {
//                        session.inboundPacket(UpdateAbilitiesPacket().apply {
//                            uniqueEntityId = entityId
//                            playerPermission = PlayerPermission.OPERATOR
//                            commandPermission = CommandPermission.OPERATOR
//                            abilityLayers.add(AbilityLayer().apply {
//                                layerType = AbilityLayer.Type.BASE
//                                abilitiesSet.addAll(Ability.values())
//                                abilityValues.addAll(arrayOf(Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES, Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS, Ability.OPERATOR_COMMANDS, Ability.MAY_FLY, Ability.FLY_SPEED, Ability.WALK_SPEED))
//                                walkSpeed = 0.1f
//                                flySpeed = 0.25f
//                            })
//                        })
//                        return false
//                    } else if (packet is UpdateAttributesPacket) {
//                        packet.attributes.add(AttributeData("minecraft:movement", 0.0f, Float.MAX_VALUE, 0.5f, 0.1f))
//                    }
                    return true
                }

                override fun onPacketOutbound(packet: BedrockPacket): Boolean {
                    if (packet !is PlayerAuthInputPacket) {
                        println(packet)
                    }
                    if (packet is RequestAbilityPacket && packet.ability == Ability.FLYING) return false
                    return true
                }
            })
            session.listener.childListener.add(RakNetRelaySessionListenerMicrosoft(getMSAccessToken(), session))
        }
    }
    relay.bind()
    println("bind")
    Thread.sleep(Long.MAX_VALUE)
}

private fun getMSAccessToken(): String {
    val file = File(".ms_refresh_token")
    val body = JsonParser.parseReader(
        HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
        "client_id=00000000441cc96b&scope=service::user.auth.xboxlive.com::MBI_SSL&grant_type=refresh_token&redirect_uri=https://login.live.com/oauth20_desktop.srf&refresh_token=${file.readText(Charsets.UTF_8)}",
        mapOf("Content-Type" to "application/x-www-form-urlencoded")).inputStream.reader(Charsets.UTF_8)).asJsonObject
    file.writeText(body.get("refresh_token").asString)
    return body.get("access_token").asString
}