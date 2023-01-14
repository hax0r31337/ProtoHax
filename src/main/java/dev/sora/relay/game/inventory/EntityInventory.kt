package dev.sora.relay.game.inventory

import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.packet.MobArmorEquipmentPacket
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket

open class EntityInventory(val entityId: Long) : AbstractInventory() {

    override val capacity: Int
        get() = 6

    open var hand: ItemData
        get() = content[0]
        set(value) { content[0] = value }
    open var offhand: ItemData
        get() = content[1]
        set(value) { content[1] = value }

    open var helmet: ItemData
        get() = content[2]
        set(value) { content[2] = value }
    open var chestplate: ItemData
        get() = content[3]
        set(value) { content[3] = value }
    open var leggings: ItemData
        get() = content[4]
        set(value) { content[4] = value }
    open var boots: ItemData
        get() = content[5]
        set(value) { content[5] = value }

    open fun handlePacket(packet: BedrockPacket) {
        if (packet is MobEquipmentPacket && packet.runtimeEntityId == entityId) {
            if (packet.containerId == 0) {
                hand = packet.item
            } else if (packet.containerId == CONTAINER_ID_OFFHAND) {
                offhand = packet.item
            }
        } else if (packet is MobArmorEquipmentPacket && packet.runtimeEntityId == entityId) {
            helmet = packet.helmet
            chestplate = packet.chestplate
            leggings = packet.leggings
            boots = packet.boots
        }
    }

    companion object {
        const val CONTAINER_ID_OFFHAND = 119
        const val CONTAINER_ID_ARMOR = 120
    }
}