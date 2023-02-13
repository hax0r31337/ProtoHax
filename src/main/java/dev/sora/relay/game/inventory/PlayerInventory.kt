package dev.sora.relay.game.inventory

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.inventory.*
import com.nukkitx.protocol.bedrock.packet.*

class PlayerInventory : EntityInventory(0L) {

    var heldItemSlot = 0
        private set

    override val capacity: Int
        get() = 41 // 36 (inventory) + 4 (armor) + 1 (off-hand)

    fun handleClientPacket(packet: BedrockPacket) {
        if (packet is PlayerHotbarPacket) {
            heldItemSlot = packet.selectedHotbarSlot
        } else if (packet is MobEquipmentPacket) {
            heldItemSlot = packet.hotbarSlot
        } else if (packet is InventoryTransactionPacket && packet.transactionType == TransactionType.NORMAL) {
            packet.actions.filter { it is InventoryActionData && it.source.type == InventorySource.Type.CONTAINER }.forEach {
                val containerId = getOffsetByContainerId(it.source.containerId)
                if (containerId == -1) return@forEach
                content[it.slot+containerId] = it.toItem
            }
        }
    }

    override fun handlePacket(packet: BedrockPacket) {
        super.handlePacket(packet)
        if (packet is PlayerHotbarPacket) {
            heldItemSlot = packet.selectedHotbarSlot
        } else if (packet is InventorySlotPacket) {
            val offset = getOffsetByContainerId(packet.containerId)
            if (offset == -1) return
            content[packet.slot+offset] = packet.item
        } else if (packet is InventoryContentPacket) {
            val offset = getOffsetByContainerId(packet.containerId)
            if (offset == -1) return
            fillContent(packet.contents, offset)
        }
    }

    private fun getOffsetByContainerId(container: Int): Int {
        return when(container) {
            0 -> 0
            ContainerId.ARMOR -> 36
            ContainerId.OFFHAND -> 40
            else -> -1
        }
    }

    private fun fillContent(contents: List<ItemData>, offset: Int) {
        contents.forEachIndexed { i, item ->
            content[offset+i] = item
        }
    }

    fun reset() {
        for (i in content.indices) {
            content[i] = ItemData.AIR
        }
    }

    override fun getNetworkSlotInfo(slot: Int): Pair<Int, Int> {
        return if (slot < 36) 0 to slot
        else if (slot < 40) ContainerId.ARMOR to slot - 36
        else if (slot == 40) ContainerId.OFFHAND to 0
        else error("invalid slot: $slot")
    }

    override fun findEmptySlot(): Int? {
        for (i in 0 until 36) {
            if (content[i] == ItemData.AIR) {
                return i
            }
        }
        return null
    }

    override var hand: ItemData
        get() = content[heldItemSlot]
        set(value) { content[heldItemSlot] = value }
    override var offhand: ItemData
        get() = content[40]
        set(value) { content[40] = value }

    override var helmet: ItemData
        get() = content[36]
        set(value) { content[36] = value }
    override var chestplate: ItemData
        get() = content[37]
        set(value) { content[37] = value }
    override var leggings: ItemData
        get() = content[38]
        set(value) { content[38] = value }
    override var boots: ItemData
        get() = content[39]
        set(value) { content[39] = value }

    companion object {
        const val SLOT_HELMET = 36
        const val SLOT_CHESTPLATE = 37
        const val SLOT_LEGGINGS = 38
        const val SLOT_BOOTS = 39
    }
}