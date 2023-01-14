package dev.sora.relay.game.inventory

import com.nukkitx.protocol.bedrock.data.inventory.ItemData

abstract class AbstractInventory {

    abstract val capacity: Int
    open val content = Array(capacity) { ItemData.AIR }

}