package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.inventory.AbstractInventory
import dev.sora.relay.game.inventory.PlayerInventory
import dev.sora.relay.game.utils.mapping.ItemMapping
import dev.sora.relay.game.utils.mapping.ItemMappingUtils
import dev.sora.relay.game.utils.mapping.isBlock
import dev.sora.relay.utils.timing.ClickTimer

class ModuleInventoryHelper : CheatModule("InventoryHelper") {

    private val stealChestValue = boolValue("StealChest", true)
    private val guiOpenValue = boolValue("GuiOpen", false)
    private val fakeOpenValue = boolValue("FakeOpen", true)
    private val autoCloseValue = boolValue("AutoClose", false)
    private val throwUnnecessaryValue = boolValue("ThrowUnnecessary", true)
    private val swingValue = listValue("Swing", arrayOf("Both", "Client", "Server", "None"), "Server")
    private val clickMaxCpsValue = intValue("ClickMaxCPS", 4, 1, 20)
    private val clickMinCpsValue = intValue("ClickMinCPS", 2, 1, 20)
    private val sortArmorValue = boolValue("Armor", true)
    private val sortSwordValue = intValue("SortSword", 0, -1, 8)
    private val sortPickaxeValue = intValue("SortPickaxe", 5, -1, 8)
    private val sortAxeValue = intValue("SortAxe", 6, -1, 8)
    private val sortBlockValue = intValue("SortBlock", 7, -1, 8)
    private val sortGAppleValue = intValue("SortGApple", 2, -1, 8)
    private val noSortNoCloseValue = boolValue("NoCloseIfNoSort", true)

    private val sortArmor = arrayOf(
        Sort(PlayerInventory.SLOT_HELMET, ItemMappingUtils.TAG_IS_HELMET),
        Sort(PlayerInventory.SLOT_CHESTPLATE, ItemMappingUtils.TAG_IS_CHESTPLATE),
        Sort(PlayerInventory.SLOT_LEGGINGS, ItemMappingUtils.TAG_IS_LEGGINGS),
        Sort(PlayerInventory.SLOT_BOOTS, ItemMappingUtils.TAG_IS_BOOTS)
    )

    private val clickTimer = ClickTimer()
    private var sorted = true

    override fun onDisable() {
        sorted = false
    }

    private fun updateClick() {
        clickTimer.update(clickMinCpsValue.get(), clickMaxCpsValue.get())
        sorted = true
    }

    @Listen
    fun onTick(event: EventTick) {
        if (!clickTimer.canClick()) {
            return
        }

        val player = event.session.thePlayer
        val mapping = event.session.itemMapping
        val openContainer = player.openContainer

        if (stealChestValue.get() && openContainer != null && openContainer !is PlayerInventory) {
            // steal items
            openContainer.content.forEachIndexed { index, item ->
                if (mapping.isNecessaryItem(item) && !mapping.hasBetterItem(item, openContainer, index, strictMode = false) && !mapping.hasBetterItem(item, player.inventory)) {
                    val slot = player.inventory.findEmptySlot() ?: return
                    openContainer.moveItem(index, slot, player.inventory, event.session)

                    updateClick()
                    return
                }
            }
        } else if (!guiOpenValue.get() || openContainer != null) {
            // drop garbage
            player.inventory.content.forEachIndexed { index, item ->
                if (item == ItemData.AIR) return@forEachIndexed
                if (mapping.hasBetterItem(item, player.inventory, index) || (throwUnnecessaryValue.get() && !mapping.isNecessaryItem(item))) {
                    player.inventory.dropItem(index, event.session)
                    // player will swing if they drop an item
                    player.swing(when(swingValue.get()) {
                        "Both" -> EntityPlayerSP.SwingMode.BOTH
                        "Client" -> EntityPlayerSP.SwingMode.CLIENTSIDE
                        "Server" -> EntityPlayerSP.SwingMode.SERVERSIDE
                        else -> EntityPlayerSP.SwingMode.NONE
                    })

                    updateClick()
                    return
                }
            }

            // sort items and wear armor
            val sorts = getSorts(player.inventory)
            sorts.forEach {
                if (it.sort(mapping, player.inventory, event.session)) {
                    updateClick()
                    return
                }
            }

            if (openContainer == null) {
                updateClick()
                sorted = false
                return
            }
        } else {
            updateClick()
            sorted = false
            return
        }

        if (autoCloseValue.get() && (!noSortNoCloseValue.get() || sorted)) {
            session.sendPacketToClient(ContainerClosePacket().apply {
                id = openContainer.containerId.toByte()
                isUnknownBool0 = true // maybe? this field is true in nukkit
            })
        }
    }

    private fun getSorts(inventory: AbstractInventory): List<Sort> {
        val sorts = mutableListOf<Sort>()
        if (sortArmorValue.get()) {
            sorts.addAll(sortArmor)
        }
        if (sortSwordValue.get() != -1) {
            sorts.add(Sort(sortSwordValue.get(), ItemMappingUtils.TAG_IS_SWORD))
        }
        if (sortPickaxeValue.get() != -1) {
            sorts.add(Sort(sortPickaxeValue.get(), ItemMappingUtils.TAG_IS_PICKAXE))
        }
        if (sortAxeValue.get() != -1) {
            sorts.add(Sort(sortAxeValue.get(), ItemMappingUtils.TAG_IS_AXE))
        }
        if (sortBlockValue.get() != -1 && !inventory.content[sortBlockValue.get()].isBlock()) {
            sorts.add(Sort(sortBlockValue.get()) { item, m ->
                if (item.isBlock()) 1f else 0f
            })
        }
        if (sortGAppleValue.get() != -1) {
            sorts.add(Sort(sortGAppleValue.get()) { item, m ->
                val name = m.map(item)
                if (name == "minecraft:golden_apple") 1f else 0f
            })
        }
        return sorts
    }

//    privat`e fun checkFakeOpen(): Boolean {
//
//    }`

    inner class Sort(val slot: Int, val judge: (ItemData, ItemMapping) -> Float) {

        constructor(slot: Int, judgeTag: String) : this(slot, { item, m ->
            val itemTags = m.tags(item)
            if (itemTags.contains(judgeTag)) {
                m.getTier(itemTags).toFloat()
            } else {
                0f
            }
        })

        fun sort(mapping: ItemMapping, inventory: AbstractInventory, session: GameSession): Boolean {
            val bestSlot = inventory.findBestItem { item ->
                judge(item, mapping)
            } ?: return false
            if (bestSlot == slot) return false

            inventory.moveItem(bestSlot, slot, inventory, session)
            return true
        }
    }
}