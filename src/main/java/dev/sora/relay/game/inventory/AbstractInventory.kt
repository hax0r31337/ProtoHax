package dev.sora.relay.game.inventory

import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
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

    open fun moveItem(sourceSlot: Int, destinationSlot: Int, destinationInventory: AbstractInventory): InventoryTransactionPacket {
        val sourceInfo = getNetworkSlotInfo(sourceSlot)
        val destinationInfo = destinationInventory.getNetworkSlotInfo(destinationSlot)
        return InventoryTransactionPacket().apply {
            transactionType = TransactionType.NORMAL
            actions.add(InventoryActionData(InventorySource.fromContainerWindowId(sourceInfo.first), sourceInfo.second,
                content[sourceSlot], destinationInventory.content[destinationSlot]))
            actions.add(InventoryActionData(InventorySource.fromContainerWindowId(destinationInfo.first), destinationInfo.second,
                destinationInventory.content[destinationSlot], content[sourceSlot]))
        }
    }

    open fun moveItem(sourceSlot: Int, destinationSlot: Int, destinationInventory: AbstractInventory, session: GameSession) {
        // send packet to server
        val pk = moveItem(sourceSlot, destinationSlot, destinationInventory)
        session.sendPacket(pk)

        // sync with client
        val sourceInfo = getNetworkSlotInfo(sourceSlot)
        session.netSession.inboundPacket(InventorySlotPacket().apply {
            containerId = sourceInfo.first
            slot = sourceInfo.second
            item = content[sourceSlot]
        })
        val destinationInfo = destinationInventory.getNetworkSlotInfo(destinationSlot)
        session.netSession.inboundPacket(InventorySlotPacket().apply {
            containerId = destinationInfo.first
            slot = destinationInfo.second
            item = destinationInventory.content[destinationSlot]
        })
    }

    open fun dropItem(slot: Int): InventoryTransactionPacket {
        val info = getNetworkSlotInfo(slot)
        return InventoryTransactionPacket().apply {
            transactionType = TransactionType.NORMAL
            actions.add(InventoryActionData(InventorySource.fromContainerWindowId(info.first), info.second,
                content[slot], ItemData.AIR))
            actions.add(InventoryActionData(InventorySource.fromWorldInteraction(InventorySource.Flag.DROP_ITEM), 0,
                ItemData.AIR, content[slot]))
        }
    }

    open fun dropItem(slot: Int, session: GameSession) {
        // send packet to server
        val pk = dropItem(slot)
        session.sendPacket(pk)

        // sync with client
        val info = getNetworkSlotInfo(slot)
        session.netSession.inboundPacket(InventorySlotPacket().also {
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
}