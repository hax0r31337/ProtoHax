package dev.sora.relay

import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.*
import com.nukkitx.protocol.bedrock.data.command.CommandPermission
import com.nukkitx.protocol.bedrock.packet.AdventureSettingsPacket
import com.nukkitx.protocol.bedrock.packet.LevelChunkPacket
import com.nukkitx.protocol.bedrock.packet.PlayerActionPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import com.nukkitx.protocol.bedrock.packet.RequestAbilityPacket
import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket
import com.nukkitx.protocol.bedrock.packet.StartGamePacket
import com.nukkitx.protocol.bedrock.packet.UpdateAbilitiesPacket
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket
import com.nukkitx.protocol.bedrock.v557.Bedrock_v557
import io.netty.util.internal.logging.InternalLoggerFactory
import java.net.InetSocketAddress
import java.util.EnumSet


fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())

    val relay = RakNetRelay(InetSocketAddress("0.0.0.0", 19132), packetCodec = Bedrock_v557.V557_CODEC)
    relay.listener = object : RakNetRelayListener {
        override fun onQuery(address: InetSocketAddress) =
            "MCPE;RakNet Relay;557;1.19.20;0;10;${relay.server.guid};Bedrock level;Survival;1;19132;19132;".toByteArray()

        override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
            return InetSocketAddress("127.0.0.1", 19136)
        }

        override fun onPrepareClientConnection(address: InetSocketAddress) {
            println(address.address.hostAddress)
            println(address.port)
        }

        override fun onSession(session: RakNetRelaySession) {
            println("SESSION")
            var entityId = 0L
            session.listener = object : RakNetRelaySessionListener(session = session) {
                override fun onPacketInbound(packet: BedrockPacket): Boolean {
//                    if (packet !is LevelChunkPacket && packet !is UpdateBlockPacket) {
//                        println(packet)
//                    }
                    if (packet is StartGamePacket) {
                        entityId = packet.runtimeEntityId
                    } else if (packet is UpdateAbilitiesPacket) {
                        session.inboundPacket(UpdateAbilitiesPacket().apply {
                            uniqueEntityId = entityId
                            playerPermission = PlayerPermission.OPERATOR
                            commandPermission = CommandPermission.OPERATOR
                            abilityLayers.add(AbilityLayer().apply {
                                layerType = AbilityLayer.Type.BASE
                                abilitiesSet.addAll(Ability.values())
                                abilityValues.addAll(arrayOf(Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES, Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS, Ability.OPERATOR_COMMANDS, Ability.MAY_FLY, Ability.FLY_SPEED, Ability.WALK_SPEED))
                                walkSpeed = 0.1f
                                flySpeed = 0.25f
                            })
                        })
                        return false
                    } else if (packet is UpdateAttributesPacket) {
                        packet.attributes.add(AttributeData("minecraft:movement", 0.0f, Float.MAX_VALUE, 0.5f, 0.1f))
                    }
                    return super.onPacketInbound(packet)
                }

                override fun onPacketOutbound(packet: BedrockPacket): Boolean {
//                    if (packet !is PlayerAuthInputPacket) {
//                        println(packet)
//                    }
                    if (packet is RequestAbilityPacket && packet.ability == Ability.FLYING) return false
                    return super.onPacketOutbound(packet)
                }
            }
        }
    }
    relay.bind()
    println("bind")
    Thread.sleep(Long.MAX_VALUE)
}