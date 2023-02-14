package dev.sora.relay.game.inventory

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.inventory.*
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.DropStackRequestActionData
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.PlaceStackRequestActionData
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import com.nukkitx.protocol.bedrock.packet.ItemStackRequestPacket
import dev.sora.relay.game.GameSession

abstract class AbstractInventory(val containerId: Int) {

    abstract val capacity: Int
    open val content = Array(capacity) { ItemData.AIR }

    /**
     * @return containerId, slotId
     */
    open fun getNetworkSlotInfo(slot: Int): Pair<Int, Int> {
        return containerId to slot
    }

    private fun getSlotTypeFromInventoryId(id: Int, slot: Int): ContainerSlotType? {
        return when(id) {
            ContainerId.INVENTORY -> (if (slot < 9) ContainerSlotType.HOTBAR else ContainerSlotType.INVENTORY)
            ContainerId.ARMOR -> ContainerSlotType.ARMOR
            ContainerId.OFFHAND -> ContainerSlotType.OFFHAND
            else -> ContainerSlotType.CONTAINER
        }
    }

    open fun moveItem(sourceSlot: Int, destinationSlot: Int, destinationInventory: AbstractInventory, serverAuthoritative: Int): BedrockPacket {
        val sourceInfo = getNetworkSlotInfo(sourceSlot)
        val destinationInfo = destinationInventory.getNetworkSlotInfo(destinationSlot)
        return if (serverAuthoritative != Int.MAX_VALUE) {
            ItemStackRequestPacket().also {
                it.requests.add(ItemStackRequest(serverAuthoritative,
                    arrayOf(PlaceStackRequestActionData(content[sourceSlot].count.toByte(),
                    StackRequestSlotInfoData(getSlotTypeFromInventoryId(sourceInfo.first, sourceInfo.second), sourceInfo.second.toByte(), content[sourceSlot].netId),
                    StackRequestSlotInfoData(getSlotTypeFromInventoryId(destinationInfo.first, destinationInfo.second), destinationInfo.second.toByte(), destinationInventory.content[destinationSlot].netId))),
                    arrayOf(), null
                ))
            }
        } else {
            InventoryTransactionPacket().apply {
                transactionType = TransactionType.NORMAL
                actions.add(InventoryActionData(InventorySource.fromContainerWindowId(sourceInfo.first), sourceInfo.second,
                    content[sourceSlot], destinationInventory.content[destinationSlot]))
                actions.add(InventoryActionData(InventorySource.fromContainerWindowId(destinationInfo.first), destinationInfo.second,
                    destinationInventory.content[destinationSlot], content[sourceSlot]))
            }
        }
    }

    open fun moveItem(sourceSlot: Int, destinationSlot: Int, destinationInventory: AbstractInventory, session: GameSession) {
        // send packet to server
        val pk = moveItem(sourceSlot, destinationSlot, destinationInventory,
            if (session.inventoriesServerAuthoritative) session.thePlayer.inventory.getRequestId() else Int.MAX_VALUE)
        session.sendPacket(pk)

        // sync with client
        val sourceInfo = getNetworkSlotInfo(sourceSlot)
        session.sendPacketToClient(InventorySlotPacket().apply {
            containerId = sourceInfo.first
            slot = sourceInfo.second
            item = content[sourceSlot]
        })
        val destinationInfo = destinationInventory.getNetworkSlotInfo(destinationSlot)
        session.sendPacketToClient(InventorySlotPacket().apply {
            containerId = destinationInfo.first
            slot = destinationInfo.second
            item = destinationInventory.content[destinationSlot]
        })
    }

    open fun dropItem(slot: Int, serverAuthoritative: Int): BedrockPacket {
        val info = getNetworkSlotInfo(slot)
        return if (serverAuthoritative != Int.MAX_VALUE) {
            ItemStackRequestPacket().also {
                it.requests.add(ItemStackRequest(serverAuthoritative,
                    arrayOf(DropStackRequestActionData(content[slot].count.toByte(), StackRequestSlotInfoData(getSlotTypeFromInventoryId(info.first, info.second), info.second.toByte(), content[slot].netId), false)),
                    arrayOf(), null
                ))
            }
        } else {
            InventoryTransactionPacket().apply {
                transactionType = TransactionType.NORMAL
                actions.add(InventoryActionData(InventorySource.fromWorldInteraction(InventorySource.Flag.DROP_ITEM), 0,
                    ItemData.AIR, content[slot]))
                actions.add(InventoryActionData(InventorySource.fromContainerWindowId(info.first), info.second,
                    content[slot], ItemData.AIR))
            }
        }
    }

    open fun dropItem(slot: Int, session: GameSession) {
        // send packet to server
        val pk = dropItem(slot,
            if (session.inventoriesServerAuthoritative) session.thePlayer.inventory.getRequestId() else Int.MAX_VALUE)
        session.sendPacket(pk)

        // sync with client
        val info = getNetworkSlotInfo(slot)
        session.sendPacketToClient(InventorySlotPacket().also {
            it.containerId = info.first
            it.slot = info.second
            it.item = content[slot]
        })
    }

    open fun searchForItem(range: IntRange, condition: (ItemData) -> Boolean): Int? {
        for (i in range) {
            if (condition(content[i])) {
                return i
            }
        }
        return null
    }

    open fun searchForItem(condition: (ItemData) -> Boolean): Int? {
        content.forEachIndexed { i, item ->
            if (condition(item)) {
                return i
            }
        }
        return null
    }

    open fun searchForItemIndexed(condition: (Int, ItemData) -> Boolean): Int? {
        content.forEachIndexed { i, item ->
            if (condition(i, item)) {
                return i
            }
        }
        return null
    }

    open fun findEmptySlot(): Int? {
        content.forEachIndexed { i, item ->
            if (item == ItemData.AIR) {
                return i
            }
        }
        return null
    }

    open fun findBestItem(judge: (ItemData) -> Float): Int? {
        var slot: Int? = null
        var credit = 0f
        content.forEachIndexed { i, item ->
            val score = judge(item)
            if (score > credit) {
                credit = score
                slot = i
            }
        }
        return slot
    }
}