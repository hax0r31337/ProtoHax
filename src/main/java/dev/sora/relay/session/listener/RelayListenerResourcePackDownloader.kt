package dev.sora.relay.session.listener

import com.google.gson.JsonParser
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelayPacketListener
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.utils.logInfo
import dev.sora.relay.utils.logWarn
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import org.cloudburstmc.protocol.bedrock.packet.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class RelayListenerResourcePackDownloader(private val session: MinecraftRelaySession, private val folder: File) : MinecraftRelayPacketListener {

	private val packs = mutableListOf<Pack>()
	private val clientAcknowledgements = mutableListOf<String>()

	init {
	    folder.mkdirs()
	}

	override fun onPacketInbound(packet: BedrockPacket): Boolean {
		if (packet is ResourcePacksInfoPacket) {
			packs.clear()
			packs.addAll(packet.resourcePackInfos.map { Pack(it) })
			packs.addAll(packet.behaviorPackInfos.map { Pack(it) })
			packs.forEach {
			}
		} else if (packet is ResourcePackChunkDataPacket) {
			val pack = packs.find { it.entry.packId == packet.packId.toString() } ?: return true
			pack.data.writerIndex(packet.progress.toInt())
			packet.data.markReaderIndex()
			pack.data.writeBytes(packet.data)
			packet.data.resetReaderIndex()
			pack.obtainedChunks++
			logInfo("[ResourcePackDownloader] ${pack.entry.packId} (${pack.obtainedChunks}/${pack.chunks})")
			if (pack.obtainedChunks >= pack.chunks) {
				packs.remove(pack)
				val file = File(folder, "${pack.entry.packId}.mcpack")
				if (pack.entry.contentKey.isNotEmpty()) {
					logWarn("[ResourcePackDownloader] Decrypting resource pack: ${pack.entry.packId}")
					pack.decrypt(file.outputStream())
				} else {
					file.writeBytes(pack.data.array())
				}

				if (packs.isEmpty()) {
					session.inboundPacket(DisconnectPacket().apply {
						kickMessage = "${GameSession.COLORED_NAME}§7 >> §fResource packs was successfully downloaded!"
					})
				}
			}
		} else if (packet is ResourcePackDataInfoPacket) {
			val pack = packs.find { it.entry.packId == packet.packId.toString() } ?: return true
			pack.chunks = packet.chunkCount.toInt()
			if (clientAcknowledgements.contains(pack.entry.packId))
				return true

			for (i in 0 until pack.chunks) {
				session.outboundPacket(ResourcePackChunkRequestPacket().apply {
					packId = packet.packId
					packVersion = pack.entry.packVersion
					chunkIndex = i
				})
			}
		}
		return super.onPacketInbound(packet)
	}

	override fun onPacketOutbound(packet: BedrockPacket): Boolean {
		if (packet is ResourcePackClientResponsePacket) {
			if (packs.isNotEmpty() && (packet.status == ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS || packet.status == ResourcePackClientResponsePacket.Status.SEND_PACKS)) {
				packet.status = ResourcePackClientResponsePacket.Status.SEND_PACKS
				packet.packIds.forEach {
					clientAcknowledgements.add(it.substring(0, it.indexOf('_')))
				}
				packet.packIds.clear()
				packet.packIds.addAll(packs.map { "${it.entry.packId}_${it.entry.packVersion}" })
			}
		}
		return super.onPacketOutbound(packet)
	}

	class Pack(val entry: ResourcePacksInfoPacket.Entry) {

		val data = Unpooled.buffer(entry.packSize.toInt())
		var chunks = -1

		var obtainedChunks = 0

		fun decrypt(outStream: FileOutputStream) {
			val cached = mutableListOf<Pair<String, ByteArray>>()
			val contents = mutableMapOf<String, ByteArray>()
			var contentsInitialized = false
			val key = entry.contentKey.toByteArray(Charsets.US_ASCII)
			val out = ZipOutputStream(outStream)

			ZipInputStream(ByteBufInputStream(data)).use { zipInputStream ->
				var zipEntry: ZipEntry? = zipInputStream.nextEntry

				while (zipEntry != null) {
					if (zipEntry.name == "contents.json" || zipEntry.name == "/contents.json") {
						zipInputStream.readNBytes(0x100)
						val json = JsonParser.parseString(aes256cfbDecrypt(zipInputStream.readBytes(), key).toString(Charsets.UTF_8)).asJsonObject.getAsJsonArray("content")
						json.forEach {
							val obj = it.asJsonObject
							contents[obj.get("path").asString] = obj.get("key").asString.toByteArray(Charsets.US_ASCII)
						}
						cached.forEach {
							out.putNextEntry(ZipEntry(it.first))
							out.write(if (contents.containsKey(it.first)) {
								aes256cfbDecrypt(it.second, contents[it.first]!!)
							} else {
								it.second
							})
							out.closeEntry()
						}
						cached.clear()

						contentsInitialized = true
					} else {
						val data = zipInputStream.readBytes()

						if (contentsInitialized) {
							out.putNextEntry(ZipEntry(zipEntry.name))
							out.write(contents[zipEntry.name]?.let {
								aes256cfbDecrypt(data, it)
							} ?: data)
							out.closeEntry()
						} else {
							cached.add(zipEntry.name to data)
						}
					}

					// Move to the next zip entry
					zipEntry = zipInputStream.nextEntry
				}

				zipInputStream.close()
			}

			out.close()
		}

		private fun aes256cfbDecrypt(data: ByteArray, key: ByteArray): ByteArray {
			val secretKeySpec = SecretKeySpec(key, "AES")
			val iv = ByteArray(16)
			System.arraycopy(key, 0, iv, 0, 16)
			val ivParameterSpec = IvParameterSpec(iv)
			val cipher = Cipher.getInstance("AES/CFB8/NoPadding")
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
			return cipher.doFinal(data)
		}
	}
}
