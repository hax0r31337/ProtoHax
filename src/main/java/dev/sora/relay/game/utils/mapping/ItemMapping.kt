package dev.sora.relay.game.utils.mapping

import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import dev.sora.relay.game.inventory.AbstractInventory

class ItemMapping(data: List<Pair<String, Int>>) : RuntimeMappingImpl(data) {

    fun map(item: ItemData): String {
        return game(item.id)
    }

    fun tags(item: ItemData): List<String> {
        return ItemMappingUtils.tags(map(item))
    }

    fun isNecessaryItem(item: ItemData): Boolean {
        if (item == ItemData.AIR) return false
        if (item.isBlock()) return true

        val itemTags = tags(item)
        return itemTags.contains(ItemMappingUtils.TAG_IS_HELMET)
                || itemTags.contains(ItemMappingUtils.TAG_IS_CHESTPLATE)
                || itemTags.contains(ItemMappingUtils.TAG_IS_LEGGINGS)
                || itemTags.contains(ItemMappingUtils.TAG_IS_BOOTS)
                || itemTags.contains(ItemMappingUtils.TAG_IS_SWORD)
                || itemTags.contains(ItemMappingUtils.TAG_IS_PICKAXE)
                || itemTags.contains(ItemMappingUtils.TAG_IS_AXE)
                || itemTags.contains(ItemMappingUtils.TAG_IS_HOE)
                || itemTags.contains(ItemMappingUtils.TAG_IS_SHOVEL)
                || itemTags.contains(ItemMappingUtils.TAG_IS_FOOD)
                || map(item) == "minecraft:shield"
    }

    fun hasBetterItem(item: ItemData, container: AbstractInventory, excludeSlot: Int = -1, strictMode: Boolean = true): Boolean {
        if (item == ItemData.AIR) return false

        val itemTags = tags(item)
        var categoryTag: String
        if (itemTags.contains(ItemMappingUtils.TAG_IS_HELMET).also { categoryTag = ItemMappingUtils.TAG_IS_HELMET }
            || itemTags.contains(ItemMappingUtils.TAG_IS_CHESTPLATE).also { categoryTag = ItemMappingUtils.TAG_IS_CHESTPLATE }
            || itemTags.contains(ItemMappingUtils.TAG_IS_LEGGINGS).also { categoryTag = ItemMappingUtils.TAG_IS_LEGGINGS }
            || itemTags.contains(ItemMappingUtils.TAG_IS_BOOTS).also { categoryTag = ItemMappingUtils.TAG_IS_BOOTS }
            || itemTags.contains(ItemMappingUtils.TAG_IS_SWORD).also { categoryTag = ItemMappingUtils.TAG_IS_SWORD }
            || itemTags.contains(ItemMappingUtils.TAG_IS_PICKAXE).also { categoryTag = ItemMappingUtils.TAG_IS_PICKAXE }
            || itemTags.contains(ItemMappingUtils.TAG_IS_AXE).also { categoryTag = ItemMappingUtils.TAG_IS_AXE }
            || itemTags.contains(ItemMappingUtils.TAG_IS_HOE).also { categoryTag = ItemMappingUtils.TAG_IS_HOE }
            || itemTags.contains(ItemMappingUtils.TAG_IS_SHOVEL).also { categoryTag = ItemMappingUtils.TAG_IS_SHOVEL }) {
            // compare category and tiers
            val itemTier = getTier(itemTags)
            container.searchForItemIndexed { i, item ->
                if (i == excludeSlot) {
                    false
                } else {
                    val alternativeTags = tags(item)
                    alternativeTags.contains(categoryTag) && (if (strictMode) getTier(alternativeTags) >= itemTier else getTier(alternativeTags) > itemTier)
                }
            }?.also { return true }
            // TODO: compare enchantment
        }
        return false
    }

    fun getTier(tags: List<String>): Int {
        return when {
            tags.contains(ItemMappingUtils.TAG_LEATHER_TIER) -> 1
            tags.contains(ItemMappingUtils.TAG_WOODEN_TIER) -> 1
            tags.contains(ItemMappingUtils.TAG_GOLDEN_TIER) -> 2
            tags.contains(ItemMappingUtils.TAG_STONE_TIER) -> 3
            tags.contains(ItemMappingUtils.TAG_CHAINMAIL_TIER) -> 3
            tags.contains(ItemMappingUtils.TAG_IRON_TIER) -> 4
            tags.contains(ItemMappingUtils.TAG_DIAMOND_TIER) -> 5
            tags.contains(ItemMappingUtils.TAG_NETHERITE_TIER) -> 6
            else -> 0
        }
    }
}

fun ItemData.isBlock(): Boolean {
    return blockRuntimeId != 0
}