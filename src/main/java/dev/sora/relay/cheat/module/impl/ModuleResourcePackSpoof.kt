package dev.sora.relay.cheat.module.impl

import com.google.gson.JsonParser
import com.nukkitx.protocol.bedrock.data.ResourcePackType
import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.Listen
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipFile


object ModuleResourcePackSpoof : CheatModule("ResourcePackSpoof") {

    private const val RESOURCE_PACK_CHUNK_SIZE = 8 * 1024

    private val acceptServerPacks = boolValue("AcceptServerPacks", false)
    var resourcePackProvider: IResourcePackProvider = EmptyResourcePackProvider()

    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet

        if (packet is ResourcePacksInfoPacket) {
            if (!acceptServerPacks.get()) {
                packet.resourcePackInfos.clear()
                packet.behaviorPackInfos.clear()
            }
            // this will make the client download the resource pack
            packet.resourcePackInfos.addAll(resourcePackProvider.getEntry())
        } else if (packet is ResourcePackStackPacket) {
            if (!acceptServerPacks.get()) {
                packet.resourcePacks.clear()
                packet.behaviorPacks.clear()
            }
            // this will make the client load the resource pack
            packet.resourcePacks.addAll(resourcePackProvider.getEntry().map {
                ResourcePackStackPacket.Entry(it.packId, it.packVersion, it.subPackName)
            })
        }
    }

    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet

        if (packet is ResourcePackClientResponsePacket) {
            if (packet.status == ResourcePackClientResponsePacket.Status.SEND_PACKS) {
                packet.packIds.map { it }.forEach {
                    val entry = resourcePackProvider.getPackById(it) ?: return@forEach
                    event.session.netSession.inboundPacket(ResourcePackDataInfoPacket().apply {
                        packId = UUID.fromString(entry.first.packId)
                        packVersion = entry.first.packVersion
                        maxChunkSize = RESOURCE_PACK_CHUNK_SIZE.toLong()
                        chunkCount = entry.first.packSize / RESOURCE_PACK_CHUNK_SIZE
                        compressedPackSize = entry.first.packSize
                        hash = MessageDigest.getInstance("SHA-256").digest(entry.second)
                        type = ResourcePackType.RESOURCE
                    })
                    packet.packIds.remove(it)
                }
                if (packet.packIds.isEmpty()) {
                    event.cancel()
                }
            }
        } else if (packet is ResourcePackChunkRequestPacket) {
            val entry = resourcePackProvider.getPackById(packet.packId.toString()) ?: return
            event.session.netSession.inboundPacket(ResourcePackChunkDataPacket().apply {
                packId = packet.packId
                packVersion = packet.packVersion
                chunkIndex = packet.chunkIndex
                progress = (RESOURCE_PACK_CHUNK_SIZE * chunkIndex).toLong()
                data = getPackChunk(entry.second, progress.toInt(), RESOURCE_PACK_CHUNK_SIZE)
            })
            event.cancel()
        }
    }


    private fun getPackChunk(data: ByteArray, off: Int, len: Int): ByteArray? {
        val chunk = if (data.size - off > len) {
            ByteArray(len)
        } else {
            ByteArray(data.size - off)
        }
        for (i in chunk.indices) {
            chunk[i] = data[off+i]
        }
        return chunk
    }


    interface IResourcePackProvider {

        fun getEntry(): Collection<ResourcePacksInfoPacket.Entry>

        fun getPackById(id: String): Pair<ResourcePacksInfoPacket.Entry, ByteArray>?
    }

    class EmptyResourcePackProvider : IResourcePackProvider {

        override fun getEntry(): Collection<ResourcePacksInfoPacket.Entry> {
            return emptyList()
        }

        override fun getPackById(id: String): Pair<ResourcePacksInfoPacket.Entry, ByteArray>? {
            return null
        }
    }

    class FileSystemResourcePackProvider(dir: File) : IResourcePackProvider {

        private val files = dir.listFiles()?.filter { it.isFile && it.canonicalPath.let { it.endsWith(".zip") || it.endsWith(".mcpack") } }
            ?.associate {
                val data = it.readBytes()
                val manifest = readManifest(it)
                ResourcePacksInfoPacket.Entry(manifest.first, manifest.second, data.size.toLong(), "", "", "", false, false) to data
            } ?: emptyMap()

        private fun readManifest(file: File): Pair<String, String> {
            val zip = ZipFile(file)
            val entry = zip.getEntry("manifest.json") ?: error("no manifest found in resource pack file: ${file.canonicalPath}")
            val manifest = JsonParser.parseReader(InputStreamReader(zip.getInputStream(entry))).asJsonObject.getAsJsonObject("header")
            return manifest.get("uuid").asString to manifest.getAsJsonArray("version").joinToString(".")
        }

        override fun getEntry(): Collection<ResourcePacksInfoPacket.Entry> {
            return files.keys
        }

        override fun getPackById(idRaw: String): Pair<ResourcePacksInfoPacket.Entry, ByteArray>? {
            val id = idRaw.split("_")[0]
            files.forEach {
                if (it.key.packId == id) {
                    return it.key to it.value
                }
            }
            return null
        }

    }
}