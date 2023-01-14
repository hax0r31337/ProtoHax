package dev.sora.relay.game.inventory

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket
import com.nukkitx.protocol.bedrock.packet.PlayerHotbarPacket
import dev.sora.relay.game.entity.EntityPlayerSP

class PlayerInventory(private val player: EntityPlayerSP) : EntityInventory(0L) {

    var heldItemSlot = 0
        private set

    override val capacity: Int
        get() = 41 // 36 (inventory) + 4 (armor) + 1 (off-hand)

    fun handleClientPacket(packet: BedrockPacket) {
        if (packet is PlayerHotbarPacket && packet.containerId == 0) {
            heldItemSlot = packet.selectedHotbarSlot
        } else if (packet is MobEquipmentPacket && packet.runtimeEntityId == entityId) {
            heldItemSlot = packet.hotbarSlot
            println(packet)
        } else if (packet is InventoryTransactionPacket) {
            packet.actions.filter { it is InventoryActionData && it.source.type == InventorySource.Type.CONTAINER }.forEach {
                val containerId = try {
                    getOffsetByContainerId(it.source.containerId)
                } catch (t: Throwable) {
                    return@forEach
                }
                content[it.slot+containerId] = it.toItem
            }
        }
    }

    override fun handlePacket(packet: BedrockPacket) {
        super.handlePacket(packet)
        if (packet is InventorySlotPacket) {
            content[packet.slot+getOffsetByContainerId(packet.containerId)] = packet.item
        } else if (packet is InventoryContentPacket) {
            fillContent(packet.contents, getOffsetByContainerId(packet.containerId))
        }
    }

    private fun getOffsetByContainerId(container: Int): Int {
        return when(container) {
            0 -> 0
            CONTAINER_ID_OFFHAND -> 40
            CONTAINER_ID_ARMOR ->36
            else -> error("invalid container id: $container")
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
}