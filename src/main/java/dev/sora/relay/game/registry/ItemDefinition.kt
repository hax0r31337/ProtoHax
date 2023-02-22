package dev.sora.relay.game.registry

import dev.sora.relay.game.inventory.AbstractInventory
import dev.sora.relay.game.utils.constants.ItemTags
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData

open class ItemDefinition(private val runtimeId: Int, private val identifier: String) :
    org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition {

    override fun getRuntimeId() = runtimeId

    override fun getIdentifier() = identifier

    override fun isComponentBased() = false
    
    val tags: List<String>
        get() = ItemTags.tags(identifier)

    fun isNecessaryItem(item: ItemData): Boolean {
        if (item == ItemData.AIR) return false
        if (item.isBlock()) return true

        val itemTags = tags
        return itemTags.contains(ItemTags.TAG_IS_HELMET)
                || itemTags.contains(ItemTags.TAG_IS_CHESTPLATE)
                || itemTags.contains(ItemTags.TAG_IS_LEGGINGS)
                || itemTags.contains(ItemTags.TAG_IS_BOOTS)
                || itemTags.contains(ItemTags.TAG_IS_SWORD)
                || itemTags.contains(ItemTags.TAG_IS_PICKAXE)
                || itemTags.contains(ItemTags.TAG_IS_AXE)
                || itemTags.contains(ItemTags.TAG_IS_HOE)
                || itemTags.contains(ItemTags.TAG_IS_SHOVEL)
                || itemTags.contains(ItemTags.TAG_IS_FOOD)
                || identifier == "minecraft:shield"
    }
    
    fun hasBetterItem(container: AbstractInventory, excludeSlot: Int = -1, strictMode: Boolean = true): Boolean {
        if (this is UnknownItemDefinition || identifier == "minecraft:air") return false

        val itemTags = tags
        var categoryTag: String
        if (itemTags.contains(ItemTags.TAG_IS_HELMET).also { categoryTag = ItemTags.TAG_IS_HELMET }
            || itemTags.contains(ItemTags.TAG_IS_CHESTPLATE).also { categoryTag = ItemTags.TAG_IS_CHESTPLATE }
            || itemTags.contains(ItemTags.TAG_IS_LEGGINGS).also { categoryTag = ItemTags.TAG_IS_LEGGINGS }
            || itemTags.contains(ItemTags.TAG_IS_BOOTS).also { categoryTag = ItemTags.TAG_IS_BOOTS }
            || itemTags.contains(ItemTags.TAG_IS_SWORD).also { categoryTag = ItemTags.TAG_IS_SWORD }
            || itemTags.contains(ItemTags.TAG_IS_PICKAXE).also { categoryTag = ItemTags.TAG_IS_PICKAXE }
            || itemTags.contains(ItemTags.TAG_IS_AXE).also { categoryTag = ItemTags.TAG_IS_AXE }
            || itemTags.contains(ItemTags.TAG_IS_HOE).also { categoryTag = ItemTags.TAG_IS_HOE }
            || itemTags.contains(ItemTags.TAG_IS_SHOVEL).also { categoryTag = ItemTags.TAG_IS_SHOVEL }) {
            // compare category and tiers
            val itemTier = getTier(itemTags)
            container.searchForItemIndexed { i, _ ->
                if (i == excludeSlot) {
                    false
                } else {
                    val alternativeTags = tags
                    alternativeTags.contains(categoryTag) && (if (strictMode) getTier(alternativeTags) >= itemTier else getTier(alternativeTags) > itemTier)
                }
            }?.also { return true }
            // TODO: compare enchantment
        }
        return false
    }

    fun getTier(tags: List<String>): Int {
        return when {
            tags.contains(ItemTags.TAG_LEATHER_TIER) -> 1
            tags.contains(ItemTags.TAG_WOODEN_TIER) -> 1
            tags.contains(ItemTags.TAG_GOLDEN_TIER) -> 2
            tags.contains(ItemTags.TAG_STONE_TIER) -> 3
            tags.contains(ItemTags.TAG_CHAINMAIL_TIER) -> 3
            tags.contains(ItemTags.TAG_IRON_TIER) -> 4
            tags.contains(ItemTags.TAG_DIAMOND_TIER) -> 5
            tags.contains(ItemTags.TAG_NETHERITE_TIER) -> 6
            else -> 0
        }
    }
}
    
class UnknownItemDefinition(runtimeId: Int): ItemDefinition(runtimeId, "minecraft:unknown")

fun ItemData.isBlock(): Boolean {
    return (blockDefinition?.runtimeId ?: 0) != 0
}

val ItemData.itemDefinition: ItemDefinition
    get() = this.definition as ItemDefinition