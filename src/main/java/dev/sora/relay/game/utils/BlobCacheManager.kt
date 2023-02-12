package dev.sora.relay.game.utils

import com.nukkitx.protocol.bedrock.packet.ClientCacheBlobStatusPacket
import com.nukkitx.protocol.bedrock.packet.ClientCacheMissResponsePacket
import dev.sora.relay.game.event.*
import dev.sora.relay.utils.logError

/**
 * manages BLOB(Binary Large OBjects) cache
 */
class BlobCacheManager : Listener {

    private val clientAcknowledgements = mutableListOf<Long>()
    private val cacheCallbacks = mutableMapOf<Long, (ByteArray) -> Unit>()

    fun registerCacheCallback(blobId: Long, callback: (ByteArray) -> Unit) {
        cacheCallbacks[blobId] = callback
    }

    @Listen
    fun onDisconnect(event: EventDisconnect) {
        clientAcknowledgements.clear()
        cacheCallbacks.clear()
    }

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet
        if (packet is ClientCacheBlobStatusPacket) {
            // sync the cache denylist
            clientAcknowledgements.addAll(packet.acks)
            clientAcknowledgements.removeIf { packet.naks.contains(it) }

            // because of we don't have such cache system, we just request cache which we required
            packet.naks.addAll(packet.acks.filter { cacheCallbacks.containsKey(it) })
            packet.acks.removeIf { packet.naks.contains(it) }
        } /*else if (packet is ResourcePacksInfoPacket) {
            // attempt disable cache
            val cacheStatusPacket = ClientCacheStatusPacket().apply {
                isSupported = false
            }
            event.session.sendPacket(cacheStatusPacket)
        } else if (packet is ClientCacheStatusPacket) {
            packet.isSupported = false
        }*/
    }

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet
        if (packet is ClientCacheMissResponsePacket) {
            // call cache callback
            packet.blobs.forEach { (id, data) ->
                cacheCallbacks[id]?.let {
                    try {
                        it(data)
                    } catch (t: Throwable) {
                        logError("cache callback", t)
                    }
                }
            }
            // prevent satisfied caches be sent to client
            packet.blobs.keys.map { it }.forEach {
                if (clientAcknowledgements.contains(it)) {
                    packet.blobs.remove(it)
                }
            }
            if (packet.blobs.isEmpty()) {
                event.cancel()
            }
        }
    }

    override fun listen() = true
}