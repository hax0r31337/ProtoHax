package dev.sora.relay.cheat.module.impl.movement

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.entity.EntityLocalPlayer
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.registry.isBlock
import dev.sora.relay.game.utils.AxisAlignedBB
import dev.sora.relay.game.utils.Rotation
import dev.sora.relay.game.utils.constants.EnumFacing
import dev.sora.relay.game.utils.toRotation
import dev.sora.relay.game.utils.toVector3f
import dev.sora.relay.game.world.Level
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId
import org.cloudburstmc.protocol.bedrock.packet.PlayerHotbarPacket

class ModuleBlockFly : CheatModule("BlockFly", CheatCategory.MOVEMENT) {

    private var swingValue by listValue("Swing", EntityLocalPlayer.SwingMode.values(), EntityLocalPlayer.SwingMode.BOTH)
    private var adaptiveBlockIdValue by boolValue("AdaptiveBlockId", false)
    private var heldBlockValue by listValue("HeldBlock", HeldBlockMode.values(), HeldBlockMode.MANUAL)
    private var rotationValue by boolValue("Rotation", false)

    private val extendableFacing = arrayOf(EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.NORTH)

    private var lastRotation: Rotation? = null

	private val handleTick = handle<EventTick> {
		if (!switchToBlock()) {
			lastRotation = null
			return@handle
		}

		if (lastRotation != null) {
			session.player.silentRotation = lastRotation
		}

		val world = session.level
		val airId = if (adaptiveBlockIdValue) {
			world.getBlockIdAt(session.player.posX.toInt(), session.player.posY.toInt(),
				session.player.posZ.toInt())
		} else {
			session.blockMapping.airId
		}
		val possibilities = searchBlocks(session.player.posX, session.player.posY - EntityPlayer.EYE_HEIGHT,
			session.player.posZ, 1, session.player, world, airId)
		val block = possibilities.firstOrNull() ?: return@handle
		val facing = getFacing(block, world, airId) ?: return@handle
		session.player.placeBlock(block, facing)
		session.player.swing(swingValue)

		if (rotationValue) {
			lastRotation = toRotation(session.player.vec3Position, block.sub(facing.unitVector).toVector3f())
			session.player.silentRotation = lastRotation
		}
	}

    private fun switchToBlock(): Boolean {
        return when(heldBlockValue) {
            HeldBlockMode.MANUAL -> session.player.inventory.hand.isBlock()
            HeldBlockMode.AUTOMATIC -> {
                if (!session.player.inventory.hand.isBlock()) {
                    val slot = session.player.inventory.searchForItem(0..8) {
                        (it.blockDefinition?.runtimeId ?: 0) != 0
                    } ?: return false
                    val packet = PlayerHotbarPacket().apply {
                        selectedHotbarSlot = slot
                        isSelectHotbarSlot = true
                        containerId = ContainerId.INVENTORY
                    }
                    session.sendPacket(packet)
                    session.sendPacketToClient(packet)
                }
                true
            }
        }
    }

    private fun searchBlocks(offsetX: Float, offsetY: Float, offsetZ: Float, range: Int, player: EntityLocalPlayer, world: Level, expected: Int): List<Vector3i> {
        val possibilities = mutableListOf<Vector3i>()
        val rangeSq = 4.5f * 4.5f
        val blockNear = mutableListOf<EnumFacing>()
        val bb = AxisAlignedBB(offsetX - .3f, offsetY - 1f, offsetZ - .3f, offsetX + .3f, offsetY + .8f, offsetZ + .3f)
		val standbb = bb.copy().apply {
			expand(-0.3f, 0f, -0.3f)
		}
		val willCollide = player.motionY < 0.05

        for (x in -range..range) {
            for (z in -range..range) {
                val pos = Vector3i.from(offsetX + x.toDouble(), offsetY - 0.625, offsetZ + z.toDouble())
				val posbb = AxisAlignedBB(pos, pos.add(1, 1, 1))
                if (world.getBlockIdAt(pos) != expected) {
					if (willCollide && standbb.intersects(posbb)) {
						return emptyList()
					} else {
						continue
					}
				} else if (pos.distanceSquared(offsetX.toDouble(), offsetY + EntityPlayer.EYE_HEIGHT.toDouble(), offsetZ.toDouble()) > rangeSq) {
					continue
				} else if (!bb.intersects(posbb)) {
					continue
				}
                EnumFacing.values().forEach {
                    val offset = pos.add(it.unitVector)
                    if (world.getBlockIdAt(offset) != expected) {
                        blockNear.add(it)
                    }
                }
                if (blockNear.size != 6 && blockNear.isNotEmpty()) {
                    possibilities.add(pos)
                }
                blockNear.clear()
            }
        }

        return possibilities
            .sortedBy { it.distanceSquared(offsetX.toDouble(), offsetY-1.0, offsetZ.toDouble()) }
    }

    private fun getFacing(block: Vector3i, world: Level, expected: Int): EnumFacing? {
        extendableFacing.forEach {
            val blockExtend = world.getBlockIdAt(block.sub(it.unitVector))
            if (blockExtend != expected) {
                return it
            }
        }
        return null
    }

    private enum class HeldBlockMode(override val choiceName: String) : NamedChoice {
        MANUAL("Manual"),
        AUTOMATIC("Auto")
    }
}
