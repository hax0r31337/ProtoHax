package dev.sora.relay.game.registry

open class BlockDefinition(private val runtimeId: Int, val identifier: String):
    org.cloudburstmc.protocol.bedrock.data.defintions.BlockDefinition {

    override fun getRuntimeId() = runtimeId
}

class UnknownBlockDefinition(runtimeId: Int): BlockDefinition(runtimeId, "minecraft:unknown")