package dev.sora.relay.game.inventory

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.inventory.*
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.DropStackRequestActionData
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.PlaceStackRequestActionData
import com.nukkitx.protocol.bedrock.packet.*
import dev.sora.relay.game.entity.EntityPlayerSP

class PlayerInventory(private val player: EntityPlayerSP) : EntityInventory(0L) {

    var heldItemSlot = 0
        private set

    override val capacity: Int
        get() = 41 // 36 (inventory) + 4 (armor) + 1 (off-hand)

    private var requestId = -1
    private val requestIdMap = mutableMapOf<Int, Int>()

    fun getRequestId(): Int {
        return requestId.also {
            requestId -= 2
        }
    }

    fun handleClientPacket(packet: BedrockPacket) {
        if (packet is PlayerHotbarPacket) {
            heldItemSlot = packet.selectedHotbarSlot
        } else if (packet is MobEquipmentPacket) {
            heldItemSlot = packet.hotbarSlot
        } else if (packet is InventoryTransactionPacket && packet.transactionType == TransactionType.NORMAL) {
            packet.actions.filter { it is InventoryActionData && it.source.type == InventorySource.Type.CONTAINER }.forEach {
                val containerId = getOffsetByContainerId(it.source.containerId) ?: return@forEach
                content[it.slot+containerId] = it.toItem
            }
        } else if (packet is ItemStackRequestPacket) {
            val newRequests = packet.requests.map {
                val newId = it.requestId
                requestIdMap[newId] = it.requestId
                ItemStackRequest(newId, it.actions, it.filterStrings, it.textProcessingEventOrigin)
            }
            packet.requests.clear()
            packet.requests.addAll(newRequests)

            processItemStackPacket(packet)
        }
    }

    private fun processItemStackPacket(packet: ItemStackRequestPacket) {
        packet.requests.forEach {
            it.actions.filterIsInstance<PlaceStackRequestActionData>().forEach { action ->
                val openContainer = player.openContainer
                val srcItem: Pair<ItemData, (ItemData) -> Unit> = if (action.source.container == ContainerSlotType.CONTAINER && openContainer is ContainerInventory) {
                    openContainer.content[action.source.slot.toInt()] to {
                        openContainer.content[action.source.slot.toInt()] = it
                    }
                } else {
                    val slot = action.source.slot.toInt() + (getOffsetByContainerType(action.source.container) ?: return@forEach)
                    content[slot] to {
                        content[slot] = it
                    }
                }
                val dstItem: Pair<ItemData, (ItemData) -> Unit> = if (action.destination.container == ContainerSlotType.CONTAINER && openContainer is ContainerInventory) {
                    openContainer.content[action.destination.slot.toInt()] to {
                        openContainer.content[action.destination.slot.toInt()] = it
                    }
                } else {
                    val slot = action.destination.slot.toInt() + (getOffsetByContainerType(action.destination.container) ?: return@forEach)
                    content[slot] to {
                        content[slot] = it
                    }
                }
                // TODO: better
                dstItem.second(srcItem.first)
                srcItem.second(dstItem.first)
            }
            it.actions.filterIsInstance<DropStackRequestActionData>().forEach { action ->
                val slot = action.source.slot.toInt() + (getOffsetByContainerType(action.source.container) ?: return@forEach)
                val item = content[slot]
                item.count -= action.count
                if (item.count == 0) {
                    content[slot] = ItemData.AIR
                }
            }
        }
    }

    override fun handlePacket(packet: BedrockPacket) {
        super.handlePacket(packet)
        if (packet is PlayerHotbarPacket) {
            heldItemSlot = packet.selectedHotbarSlot
        } else if (packet is InventorySlotPacket) {
            val offset = getOffsetByContainerId(packet.containerId) ?: return
            content[packet.slot+offset] = packet.item
        } else if (packet is InventoryContentPacket) {
            val offset = getOffsetByContainerId(packet.containerId) ?: return
            fillContent(packet.contents, offset)
        } else if (packet is ItemStackResponsePacket) {
            val newResponse = packet.entries.map {
                val oldId = requestIdMap[it.requestId]?.also { _ -> requestIdMap.remove(it.requestId) } ?: it.requestId
                ItemStackResponsePacket.Response(it.result, oldId, it.containers)
            }
            packet.entries.clear()
            packet.entries.addAll(newResponse)
        }
    }

    private fun getOffsetByContainerId(container: Int): Int? {
        return when(container) {
            0 -> 0
            ContainerId.ARMOR -> 36
            ContainerId.OFFHAND -> 40
            else -> null
        }
    }

    private fun getOffsetByContainerType(container: ContainerSlotType): Int? {
        return when(container) {
            ContainerSlotType.INVENTORY -> 0
            ContainerSlotType.ARMOR -> 36
            ContainerSlotType.OFFHAND -> 40
            else -> null
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