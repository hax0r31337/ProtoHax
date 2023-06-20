package dev.sora.relay.cheat.module.impl.misc

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.Choice
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.EventEntityDespawn
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.utils.getRandomString
import dev.sora.relay.utils.timing.TheTimer
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import kotlin.random.Random

class ModuleSpammer : CheatModule("Spammer", CheatCategory.MISC) {

	private var modeValue by choiceValue("Mode", arrayOf(Spam(), KillSay()), "Spam")
    private var messageValue by stringValue("Message", "[!] I'm using ProtoHax t<dot>me/protohax")
	private var randomSuffix by boolValue("RandomSuffix", true)

	private fun sendMessage(placeholders: Map<String, String>) {
		session.sendPacket(TextPacket().apply {
			type = TextPacket.Type.CHAT
			xuid = session.player.xuid
			sourceName = session.player.username
			platformChatId = ""
			message = messageValue.let { if (randomSuffix) "$it >${getRandomString(10 + Random.nextInt(5))}<" else it }.let {
				var result = it
				placeholders.forEach { (k, v) -> result = result.replace(k, v) }
				result
			}
		})
	}

	private inner class Spam : Choice("Spam") {

		private var delayValue by intValue("Delay", 5000, 500..10000)

		private val spamTimer = TheTimer()

		private val handleTick = handle<EventTick> {
			if (spamTimer.hasTimePassed(delayValue)) {
				sendMessage(emptyMap())
				spamTimer.reset()
			}
		}
	}

	private inner class KillSay : Choice("KillSay") {

		private var lastAttack: Long? = null

		override fun onDisable() {
			lastAttack = null
		}

		private val handleEntityDespawn = handle<EventEntityDespawn> {
			if (entity is EntityPlayer && entity.runtimeEntityId == lastAttack) {
				sendMessage(mapOf("\$name" to entity.username))
			}
		}

		private val handlePacketOutbound = handle<EventPacketOutbound> {
			if (packet is InventoryTransactionPacket && packet.transactionType == InventoryTransactionType.ITEM_USE_ON_ENTITY && packet.actionType == 1) {
				lastAttack = packet.runtimeEntityId
			}
		}
	}
}
