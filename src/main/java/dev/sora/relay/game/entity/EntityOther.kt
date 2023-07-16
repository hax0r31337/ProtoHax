package dev.sora.relay.game.entity

class EntityOther(runtimeEntityId: Long, uniqueEntityId: Long, val identifier: String)
	: Entity(runtimeEntityId, uniqueEntityId) {

	val isHostile = hostileEntities.contains(identifier)
	val isNeutral = hostileEntities.contains(identifier)

	val isLiving = isHostile || isNeutral


	override fun toString(): String {
		return "EntityOther(entityId=$runtimeEntityId, uniqueId=$uniqueEntityId, identifier=$identifier, posX=$posX, posY=$posY, posZ=$posZ)"
	}

	companion object {
		val hostileEntities = listOf(
			"minecraft:wither_skeleton",
			"minecraft:husk",
			"minecraft:stray",
			"minecraft:witch",
			"minecraft:zombie_villager",
			"minecraft:blaze",
			"minecraft:magma_cube",
			"minecraft:ghast",
			"minecraft:cave_spider",
			"minecraft:silverfish",
			"minecraft:enderman",
			"minecraft:slime",
			"minecraft:zombie_pigman",
			"minecraft:spider",
			"minecraft:skeleton",
			"minecraft:creeper",
			"minecraft:zombie",
			"minecraft:skeleton_horse",
			"minecraft:wolf",
			"minecraft:drowned",
			"minecraft:guardian",
			"minecraft:elder_guardian",
			"minecraft:vindicator",
			"minecraft:wither",
			"minecraft:ender_dragon",
			"minecraft:shulker",
			"minecraft:endermite",
			"minecraft:evocation_illager",
			"minecraft:vex",
			"minecraft:phantom",
			"minecraft:pillager",
			"minecraft:ravager",
			"minecraft:fox",
			"minecraft:piglin",
			"minecraft:hoglin",
			"minecraft:zoglin",
			"minecraft:piglin_brute",
			"minecraft:warden"
		)

		val neutralEntities = listOf(
			"minecraft:mule",
			"minecraft:donkey",
			"minecraft:dolphin",
			"minecraft:tropicalfish",
			"minecraft:squid",
			"minecraft:sheep",
			"minecraft:mooshroom",
			"minecraft:panda",
			"minecraft:salmon",
			"minecraft:pig",
			"minecraft:villager",
			"minecraft:cod",
			"minecraft:pufferfish",
			"minecraft:cow",
			"minecraft:chicken",
			"minecraft:llama",
			"minecraft:iron_golem",
			"minecraft:rabbit",
			"minecraft:snow_golem",
			"minecraft:bat",
			"minecraft:ocelot",
			"minecraft:horse",
			"minecraft:cat",
			"minecraft:polar_bear",
			"minecraft:zombie_horse",
			"minecraft:turtle",
			"minecraft:parrot",
			"minecraft:wandering_trader",
			"minecraft:villager_v2",
			"minecraft:zombie_villager_v2",
			"minecraft:bee",
			"minecraft:strider",
			"minecraft:goat",
			"minecraft:glow_squid",
			"minecraft:axolotl",
			"minecraft:frog",
			"minecraft:tadpole",
			"minecraft:allay"
		)
	}
}
