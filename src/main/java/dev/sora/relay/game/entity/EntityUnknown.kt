package dev.sora.relay.game.entity

class EntityUnknown(runtimeEntityId: Long, uniqueEntityId: Long, val identifier: String)
	: Entity(runtimeEntityId, uniqueEntityId) {

	override fun toString(): String {
		return "EntityUnknown(entityId=$runtimeEntityId, uniqueId=$uniqueEntityId, identifier=$identifier, posX=$posX, posY=$posY, posZ=$posZ)"
	}
}
