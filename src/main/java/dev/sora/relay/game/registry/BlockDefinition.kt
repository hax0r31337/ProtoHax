package dev.sora.relay.game.registry

import org.cloudburstmc.nbt.NbtMap

open class BlockDefinition(private var runtimeId: Int, val identifier: String, val states: NbtMap):
    org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition {

    override fun getRuntimeId() = runtimeId

	fun setRuntimeId(runtimeId: Int) {
		this.runtimeId = runtimeId
	}

	override fun toString(): String {
		return identifier
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		if (other !is BlockDefinition) return false

		if (identifier != other.identifier) return false
		return states == other.states
	}

	override fun hashCode(): Int {
		var result = identifier.hashCode()
		result = 31 * result + states.hashCode()
		return result
	}
}

class UnknownBlockDefinition(runtimeId: Int): BlockDefinition(runtimeId, "minecraft:unknown", NbtMap.EMPTY)
