package dev.sora.relay.game.inventory

import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryActionData
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventorySource
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket

class ContainerInventory(containerId: Int, val type: ContainerType) : AbstractInventory(containerId) {

	override var content = emptyArray<ItemData>()

    fun handleClientPacket(packet: BedrockPacket) {
        if (packet is InventoryTransactionPacket) {
            packet.actions.filter { it is InventoryActionData &&
                    it.source.type == InventorySource.Type.CONTAINER &&
                    it.source.containerId == containerId }.forEach {
                content[it.slot] = it.toItem
            }
        }
    }

    fun handlePacket(packet: BedrockPacket) {
        if (packet is InventoryContentPacket && packet.containerId == containerId) {
            content = packet.contents.toTypedArray()
        } else if (packet is InventorySlotPacket && packet.containerId == containerId) {
            content[packet.slot] = packet.item
        }
    }
}
