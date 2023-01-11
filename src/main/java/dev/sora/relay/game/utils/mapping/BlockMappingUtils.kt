package dev.sora.relay.game.utils.mapping

import com.nukkitx.nbt.NBTInputStream
import com.nukkitx.nbt.NbtList
import com.nukkitx.nbt.NbtMap
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

object BlockMappingUtils : AbstractMappingUtils() {

    override val resourcePath: String
        get() = "/assets/mcpedata/blocks"

    override fun readMapping(version: Short, vararg options: String): RuntimeMapping {
        if (!availableVersions.contains(version)) return emptyMapping

        val tag = NBTInputStream(DataInputStream(
            GZIPInputStream(AbstractMappingUtils::class.java.getResourceAsStream("${resourcePath}/runtime_block_states_$version.dat"))
        )).readTag() as NbtList<NbtMap>
        val runtimeToBlock = mutableMapOf<Int, String>()
        val blockToRuntime = mutableMapOf<String, Int>()

        if (options.contains("legacy")) {
            tag.forEach { subtag ->
                val name = getBlockNameFromNbt(subtag)
                val runtime = subtag.getInt("id") shl 6 or subtag.getShort("data").toInt()

                runtimeToBlock[runtime] = name
            }
        } else {
            tag.forEach { subtag ->
                val name = getBlockNameFromNbt(subtag)
                val runtime = subtag.getInt("runtimeId")

                runtimeToBlock[runtime] = name
                blockToRuntime[name] = runtime
            }
        }

        return RuntimeMappingImpl(runtimeToBlock, blockToRuntime)
    }

    override fun readMapping(version: Short): RuntimeMapping {
        return readMapping(version, *emptyArray())
    }

    fun getBlockNameFromNbt(nbt: NbtMap): String {
        val sb = StringBuilder()
        sb.append(nbt.getString("name"))
        val stateMap = (nbt.getCompound("states") ?: NbtMap.builder().build())
            .map { it }.sortedBy { it.key }
        if(stateMap.isNotEmpty()) {
            sb.append("[")
            stateMap.forEach { (key, value) ->
                sb.append(key)
                sb.append("=")
                sb.append(value)
                sb.append(",")
            }
            sb.delete(sb.length - 1, sb.length)
            sb.append("]")
        }
        return sb.toString()
    }
}