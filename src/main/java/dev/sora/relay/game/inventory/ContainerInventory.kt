package dev.sora.relay.game.inventory

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket

class ContainerInventory(containerId: Int, val type: ContainerType) : AbstractInventory(containerId) {

    override val capacity: Int
        get() = content?.size ?: 0
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