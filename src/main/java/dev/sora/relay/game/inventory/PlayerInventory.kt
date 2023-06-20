package dev.sora.relay.game.inventory

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.EntityLocalPlayer
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.PlaceAction
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.*
import java.util.*

class PlayerInventory(private val player: EntityLocalPlayer) : EntityInventory(player) {

	// 36 (inventory) + 4 (armor) + 1 (off-hand)
	override val content = Array(41) { ItemData.AIR }

    var heldItemSlot = 0
        private set

    private var requestId = -1
    private val requestIdMap = mutableMapOf<Int, Int>()
    private val pendingRequests = LinkedList<ItemStackRequest>()

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
        } else if (packet is InventoryTransactionPacket && packet.transactionType == InventoryTransactionType.NORMAL) {
            packet.actions.filter { it is InventoryActionData && it.source.type == InventorySource.Type.CONTAINER }.forEach {
                val containerId = getOffsetByContainerId(it.source.containerId) ?: return@forEach
                content[it.slot+containerId] = it.toItem
            }
        } else if (packet is ItemStackRequestPacket) {
            val newRequests = packet.requests.map {
                val newId = requestId
                requestIdMap[newId] = it.requestId
                ItemStackRequest(newId, it.actions, it.filterStrings, it.textProcessingEventOrigin)
            }
            packet.requests.clear()
            packet.requests.addAll(newRequests)

            processItemStackPacket(packet)
        } else if (packet is PlayerAuthInputPacket) {
            if (packet.inputData.contains(PlayerAuthInputData.PERFORM_ITEM_STACK_REQUEST)) {
                packet.itemStackRequest = packet.itemStackRequest.let {
                    val newId = requestId
                    requestIdMap[newId] = it.requestId
                    ItemStackRequest(newId, it.actions, it.filterStrings, it.textProcessingEventOrigin)
                }
            } else if (pendingRequests.isNotEmpty()) {
                packet.itemStackRequest = pendingRequests.pop()
                packet.inputData.add(PlayerAuthInputData.PERFORM_ITEM_STACK_REQUEST)
            }
        }
    }

    /**
     * only call this if player.inventoriesServerAuthoritative == true
     */
    fun itemStackRequest(request: ItemStackRequest, session: GameSession) {
        assert(player.inventoriesServerAuthoritative) { "inventory action is not server authoritative" }
        if (player.movementServerAuthoritative) {
            pendingRequests.add(request)
        } else {
            session.sendPacket(ItemStackRequestPacket().also {
                it.requests.add(request)
            })
        }
    }

    private fun processItemStackPacket(packet: ItemStackRequestPacket) {
        packet.requests.forEach {
            it.actions.filterIsInstance<PlaceAction>().forEach FE@ { action ->
                val openContainer = player.openContainer
                val srcItem: Pair<ItemData, (ItemData) -> Unit> = if (action.source.container == ContainerSlotType.LEVEL_ENTITY && openContainer is ContainerInventory) {
                    openContainer.content[action.source.slot] to {
                        openContainer.content[action.source.slot] = it
                    }
                } else {
                    val slot = action.source.slot + (getOffsetByContainerType(action.source.container) ?: return@FE)
                    content[slot] to {
                        content[slot] = it
                    }
                }
                val dstItem: Pair<ItemData, (ItemData) -> Unit> = if (action.destination.container == ContainerSlotType.LEVEL_ENTITY && openContainer is ContainerInventory) {
                    openContainer.content[action.destination.slot] to {
                        openContainer.content[action.destination.slot] = it
                    }
                } else {
                    val slot = action.destination.slot + (getOffsetByContainerType(action.destination.container) ?: return@FE)
                    content[slot] to {
                        content[slot] = it
                    }
                }
                dstItem.second(srcItem.first)
                srcItem.second(dstItem.first)
            }
            it.actions.filterIsInstance<DropAction>().forEach FE@ { action ->
                val slot = action.source.slot + (getOffsetByContainerType(action.source.container) ?: return@FE)
                val item = content[slot]
                if (item.count == 1) {
                    content[slot] = ItemData.AIR
                } else {
                    content[slot] = item.toBuilder()
                        .count(item.count - 1)
                        .build()
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
                ItemStackResponse(it.result, oldId, it.containers)
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
			ContainerSlotType.HOTBAR -> 27
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

	fun searchForItemInHotbar(condition: (ItemData) -> Boolean): Int? {
		for (i in 0 until 9) {
			if (condition(content[i])) {
				return i
			}
		}
		return null
	}

	fun searchForItemIndexedInHotbar(condition: (Int, ItemData) -> Boolean): Int? {
		for (i in 0 until 9) {
			if (condition(i, content[i])) {
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
        const val SLOT_OFFHAND = 40
    }
}
