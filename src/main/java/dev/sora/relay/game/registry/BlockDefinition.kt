package dev.sora.relay.game.registry

import org.cloudburstmc.nbt.NbtMap

open class BlockDefinition(private val runtimeId: Int, val identifier: String, val extraData: NbtMap):
    org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition {

    override fun getRuntimeId() = runtimeId
}

class UnknownBlockDefinition(runtimeId: Int): BlockDefinition(runtimeId, "minecraft:unknown", NbtMap.EMPTY)
