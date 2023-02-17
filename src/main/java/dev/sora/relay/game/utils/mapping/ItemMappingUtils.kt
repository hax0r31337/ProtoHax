package dev.sora.relay.game.utils.mapping

import com.google.gson.JsonParser

object ItemMappingUtils : AbstractMappingUtils() {

    const val TAG_SOUL_FIRE_BASE_BLOCKS = "minecraft:soul_fire_base_blocks"
    const val TAG_IS_TOOL = "minecraft:is_tool"
    const val TAG_IS_PICKAXE = "minecraft:is_pickaxe"
    const val TAG_DIGGER = "minecraft:digger"
    const val TAG_NETHERITE_TIER = "minecraft:netherite_tier"
    const val TAG_IS_SHOVEL = "minecraft:is_shovel"
    const val TAG_IS_HOE = "minecraft:is_hoe"
    const val TAG_DIAMOND_TIER = "minecraft:diamond_tier"
    const val TAG_IS_SWORD = "minecraft:is_sword"
    const val TAG_IRON_TIER = "minecraft:iron_tier"
    const val TAG_IS_AXE = "minecraft:is_axe"
    const val TAG_WOODEN_TIER = "minecraft:wooden_tier"
    const val TAG_GOLDEN_TIER = "minecraft:golden_tier"
    const val TAG_STONE_TIER = "minecraft:stone_tier"
    const val TAG_STONE_TOOL_MATERIALS = "minecraft:stone_tool_materials"
    const val TAG_STONE_CRAFTING_MATERIALS = "minecraft:stone_crafting_materials"
    const val TAG_CRIMSON_STEMS = "minecraft:crimson_stems"
    const val TAG_LOGS = "minecraft:logs"
    const val TAG_VIBRATION_DAMPER = "minecraft:vibration_damper"
    const val TAG_WOOL = "minecraft:wool"
    const val TAG_LEATHER_TIER = "minecraft:leather_tier"
    const val TAG_IS_ARMOR = "minecraft:is_armor"
    const val TAG_IS_HELMET = "minecraft:is_helmet"
    const val TAG_IS_CHESTPLATE = "minecraft:is_chestplate"
    const val TAG_IS_LEGGINGS = "minecraft:is_leggings"
    const val TAG_IS_BOOTS = "minecraft:is_boots"
    const val TAG_BOATS = "minecraft:boats"
    const val TAG_BOAT = "minecraft:boat"
    const val TAG_MANGROVE_LOGS = "minecraft:mangrove_logs"
    const val TAG_LOGS_THAT_BURN = "minecraft:logs_that_burn"
    const val TAG_PLANKS = "minecraft:planks"
    const val TAG_ARROW = "minecraft:arrow"
    const val TAG_CHAINMAIL_TIER = "minecraft:chainmail_tier"
    const val TAG_WARPED_STEMS = "minecraft:warped_stems"
    const val TAG_LECTERN_BOOKS = "minecraft:lectern_books"
    const val TAG_STONE_BRICKS = "minecraft:stone_bricks"
    const val TAG_HANGING_ACTOR = "minecraft:hanging_actor"
    const val TAG_DOOR = "minecraft:door"
    const val TAG_IS_MINECART = "minecraft:is_minecart"
    const val TAG_SIGN = "minecraft:sign"
    const val TAG_COALS = "minecraft:coals"
    const val TAG_WOODEN_SLABS = "minecraft:wooden_slabs"
    const val TAG_BANNER = "minecraft:banner"
    const val TAG_IS_COOKED = "minecraft:is_cooked"
    const val TAG_IS_MEAT = "minecraft:is_meat"
    const val TAG_IS_FOOD = "minecraft:is_food"
    const val TAG_IS_FISH = "minecraft:is_fish"
    const val TAG_SPAWN_EGG = "minecraft:spawn_egg"
    const val TAG_SAND = "minecraft:sand"
    const val TAG_MUSIC_DISC = "minecraft:music_disc"
    const val TAG_HORSE_ARMOR = "minecraft:horse_armor"

    private val itemTags = mutableMapOf<String, List<String>>()

    init {
        // load item tags
        val json = JsonParser.parseReader(AbstractMappingUtils::class.java.getResourceAsStream("/assets/mcpedata/item_tags.json").reader(Charsets.UTF_8)).asJsonObject
        json.entrySet().forEach { (item, tags) ->
            itemTags[item] = tags.asJsonArray.map { it.asString }
        }
    }

    override val resourcePath: String
        get() = "/assets/mcpedata/items"

    override fun readMapping(version: Short): RuntimeMapping {
        if (!availableVersions.contains(version)) return emptyMapping

        val mapping = JsonParser
            .parseReader(AbstractMappingUtils::class.java.getResourceAsStream("$resourcePath/runtime_item_states_$version.json").reader(Charsets.UTF_8))
            .asJsonArray.map {
                val obj = it.asJsonObject
                obj.get("name").asString to obj.get("id").asInt
            }
        return ItemMapping(mapping)
    }

    override fun craftMapping(protocolVersion: Int, vararg options: String): ItemMapping {
        return super.craftMapping(protocolVersion, *options) as ItemMapping
    }

    fun tags(item: String): List<String> {
        return itemTags[item] ?: emptyList()
    }
}