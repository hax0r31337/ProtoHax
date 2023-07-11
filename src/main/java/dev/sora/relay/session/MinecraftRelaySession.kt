package dev.sora.relay.session

import com.google.gson.JsonParser
import dev.sora.relay.utils.base64Decode
import dev.sora.relay.utils.logError
import dev.sora.relay.utils.logInfo
import io.netty.util.ReferenceCountUtil
import kotlinx.coroutines.*
import org.cloudburstmc.protocol.bedrock.BedrockClientSession
import org.cloudburstmc.protocol.bedrock.BedrockPeer
import org.cloudburstmc.protocol.bedrock.BedrockServerSession
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.ClientToServerHandshakePacket
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils
import java.security.KeyPair

class MinecraftRelaySession(peer: BedrockPeer, subClientId: Int) : BedrockServerSession(peer, subClientId) {

    var client: MinecraftRelayClientSession? = null
        set(value) {
            value?.let {
                it.codec = codec
				it.peer.codecHelper.blockDefinitions = peer.codecHelper.blockDefinitions
				it.peer.codecHelper.itemDefinitions = peer.codecHelper.itemDefinitions
                queuedPackets.forEach { packet ->
                    it.sendPacket(packet)
                }
                queuedPackets.clear()
            }
            field = value
        }

    private val queuedPackets = mutableListOf<BedrockPacket>()
    val listeners = mutableListOf<MinecraftRelayPacketListener>()

	var keyPair: KeyPair? = null
	var multithreadingSupported = false

	@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
	private val scope = CoroutineScope(newSingleThreadContext("RakRelay") + SupervisorJob())

    init {
        packetHandler = SessionCloseHandler {
            logInfo("client disconnect: $it")
            try {
                client?.disconnect()
                listeners.forEach { l ->
                    try {
                        l.onDisconnect(true, it)
                    } catch (t: Throwable) {
                        logError("disconnect handle", t)
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    override fun onPacket(wrapper: BedrockPacketWrapper) {
		val packet = wrapper.packet
		ReferenceCountUtil.retain(packet)

		scope.launch {
			listeners.forEach { l ->
				try {
					if (!l.onPacketOutbound(packet)) {
						return@launch
					}
				} catch (t: Throwable) {
					logError("packet outbound", t)
				}
			}

			outboundPacket(packet)

			listeners.forEach { l ->
				try {
					l.onPacketPostOutbound(packet)
				} catch (t: Throwable) {
					logError("packet outbound", t)
				}
			}
		}
    }

    override fun setCodec(codec: BedrockCodec) {
        client?.codec = codec
        super.setCodec(codec)
    }

    fun outboundPacket(packet: BedrockPacket) {
		if (client == null) {
			queuedPackets.add(packet)
		} else {
			client!!.sendPacket(packet)
		}
    }

    fun inboundPacket(packet: BedrockPacket) {
        sendPacket(packet)
    }

	override fun disconnect(reason: String?, hideReason: Boolean) {
		close(reason)
	}

	fun disconnectWithPacket(reason: String) {
		super.disconnect(reason, false)
	}

    inner class MinecraftRelayClientSession(peer: BedrockPeer, subClientId: Int) : BedrockClientSession(peer, subClientId) {

        init {
            packetHandler = SessionCloseHandler {
                logInfo("server disconnect: $it")
                try {
                    this@MinecraftRelaySession.disconnect()
                    listeners.forEach { l ->
                        try {
                            l.onDisconnect(true, it)
                        } catch (t: Throwable) {
                            logError("disconnect handle", t)
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

		private fun handlePacket(packet: BedrockPacket) {
			listeners.forEach { l ->
				try {
					if (!l.onPacketInbound(packet)) {
						return
					}
				} catch (t: Throwable) {
					logError("packet inbound", t)
				}
			}

			inboundPacket(packet)
		}

        override fun onPacket(wrapper: BedrockPacketWrapper) {
			val packet = wrapper.packet
			ReferenceCountUtil.retain(packet)

			if (packet is ServerToClientHandshakePacket && keyPair != null) {
				val jwtSplit = packet.jwt.split(".")
				val headerObject = JsonParser.parseString(base64Decode(jwtSplit[0]).toString(Charsets.UTF_8)).asJsonObject
				val payloadObject = JsonParser.parseString(base64Decode(jwtSplit[1]).toString(Charsets.UTF_8)).asJsonObject
				val serverKey = EncryptionUtils.parseKey(headerObject.get("x5u").asString)
				val key = EncryptionUtils.getSecretKey(keyPair!!.private, serverKey,
					base64Decode(payloadObject.get("salt").asString)
				)
				enableEncryption(key)
				outboundPacket(ClientToServerHandshakePacket())
			} else if (multithreadingSupported) {
				scope.launch {
					handlePacket(packet)
				}
			} else {
				handlePacket(packet)
			}
        }
    }
}
