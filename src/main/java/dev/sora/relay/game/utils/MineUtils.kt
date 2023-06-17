package dev.sora.relay.game.utils

import com.google.gson.JsonParser
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.data.Effect
import dev.sora.relay.game.registry.BlockDefinition
import dev.sora.relay.game.registry.MappingProvider
import dev.sora.relay.game.utils.constants.Enchantment
import kotlin.math.round

/**
 * utility class for mining blocks
 */
object MineUtils {

	private val hardnessMap: Map<String, Float>

	init {
		val json = JsonParser.parseReader(MappingProvider::class.java.getResourceAsStream("/assets/mcpedata/block_hardness.json").reader(Charsets.UTF_8)).asJsonObject
		val map = mutableMapOf<String, Float>()

		json.entrySet().forEach { (k, v) ->
			map[k] = v.asFloat
		}

	    hardnessMap = map
	}

	/**
	 * @return ticks to break the certain block
	 */
	fun calculateBreakTime(session: GameSession, block: BlockDefinition): Int {
		var speedMultiplier = 1f

		// TODO: tool speed multiplier

		session.player.getEffectById(Effect.HASTE)?.let {
			speedMultiplier *= 0.2f * it.amplifier + 1
		}

		session.player.getEffectById(Effect.MINING_FATIGUE)?.let {
			speedMultiplier *= when (it.amplifier) {
				0 -> 0.3f
				1 -> 0.09f
				2 -> 0.0027f
				else -> 0.00081f
			}
		}

		val pos = session.player.vec3PositionFeet.toVector3iFloor()

		// in water check
		if ((session.level.getBlockAt(pos.x, pos.y, pos.z).identifier.contains("water")
			|| session.level.getBlockAt(pos.x, pos.y + 1, pos.z).identifier.contains("water"))
			&& !session.player.inventory.hand.hasEnchant(Enchantment.WATER_WORKER)) {
			speedMultiplier /= 5
		}

		if (!session.player.onGround) {
			speedMultiplier /= 5
		}

		val hardness = hardnessMap[block.identifier] ?: 1f
		var damage = speedMultiplier / hardness

		damage /= 100

		if (damage > 1) {
			return 0
		}

		return round(1 / damage).toInt()
	}

}
