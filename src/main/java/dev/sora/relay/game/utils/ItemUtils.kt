package dev.sora.relay.game.utils

import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData

fun ItemData.removeNetInfo(): ItemData {
	return toBuilder()
		.usingNetId(false)
		.netId(0)
		.build()
}
