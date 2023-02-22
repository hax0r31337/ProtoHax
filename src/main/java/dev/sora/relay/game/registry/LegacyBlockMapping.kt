package dev.sora.relay.game.registry

import org.cloudburstmc.nbt.NBTInputStream
import org.cloudburstmc.nbt.NbtList
import org.cloudburstmc.nbt.NbtMap
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

class LegacyBlockMapping(private val stateToRuntimeMap: Map<Int, Int>) {

    fun toRuntime(id: Int, meta: Int)
        = toRuntime(id shl 6 or meta)

    fun toRuntime(state: Int): Int {
        return stateToRuntimeMap[state] ?: 0
    }

    object Provider : MappingProvider<LegacyBlockMapping>() {

        override val resourcePath: String
            get() = "/assets/mcpedata/blocks"

        override fun readMapping(version: Short): LegacyBlockMapping {
            if (!availableVersions.contains(version)) error("Version not available: $version")

            val tag = NBTInputStream(
                DataInputStream(
                GZIPInputStream(MappingProvider::class.java.getResourceAsStream("${resourcePath}/runtime_block_states_$version.dat"))
            )
            ).readTag() as NbtList<NbtMap>
            val stateToRuntime = mutableMapOf<Int, Int>()

            tag.forEach { subtag ->
                val state = subtag.getInt("id") shl 6 or subtag.getShort("data").toInt()
                val runtime = subtag.getInt("runtimeId")

                stateToRuntime[state] = runtime
            }

            return LegacyBlockMapping(stateToRuntime)
        }
    }
}