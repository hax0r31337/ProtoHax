package dev.sora.relay.cheat.module.impl

import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.registry.isBlock
import dev.sora.relay.game.utils.AxisAlignedBB
import dev.sora.relay.game.utils.Rotation
import dev.sora.relay.game.utils.constants.EnumFacing
import dev.sora.relay.game.utils.toRotation
import dev.sora.relay.game.utils.toVector3f
import dev.sora.relay.game.world.WorldClient
import org.cloudburstmc.math.vector.Vector3i
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId
import org.cloudburstmc.protocol.bedrock.packet.PlayerHotbarPacket

class ModuleBlockFly : CheatModule("BlockFly") {

    private var swingValue by listValue("Swing", EntityPlayerSP.SwingMode.values(), EntityPlayerSP.SwingMode.BOTH)
    private var adaptiveBlockIdValue by boolValue("AdaptiveBlockId", false)
    private var heldBlockValue by listValue("HeldBlock", HeldBlockMode.values(), HeldBlockMode.MANUAL)
    private var rotationValue by boolValue("Rotation", false)

    private val extendableFacing = arrayOf(EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.NORTH)

    private var lastRotation: Rotation? = null

	private val handleTick = handle<EventTick> { event ->
		val session = event.session
		if (!switchToBlock()) {
			lastRotation = null
			return@handle
		}

		if (lastRotation != null) {
			session.thePlayer.silentRotation = lastRotation
		}

		val world = session.theWorld
		val airId = if (adaptiveBlockIdValue) {
			world.getBlockIdAt(session.thePlayer.posX.toInt(), session.thePlayer.posY.toInt(),
				session.thePlayer.posZ.toInt())
		} else {
			session.blockMapping.airId
		}
		val possibilities = searchBlocks(session.thePlayer.posX, session.thePlayer.posY - EntityPlayer.EYE_HEIGHT,
			session.thePlayer.posZ, 1, world, airId)
		val block = possibilities.firstOrNull() ?: return@handle
		val facing = getFacing(block, world, airId) ?: return@handle
		session.thePlayer.placeBlock(block, facing)
		session.thePlayer.swing(swingValue)

		if (rotationValue) {
			lastRotation = toRotation(session.thePlayer.vec3Position, block.sub(facing.unitVector).toVector3f())
			session.thePlayer.silentRotation = lastRotation
		}
	}

    private fun switchToBlock(): Boolean {
        return when(heldBlockValue) {
            HeldBlockMode.MANUAL -> session.thePlayer.inventory.hand.isBlock()
            HeldBlockMode.AUTOMATIC -> {
                if (!session.thePlayer.inventory.hand.isBlock()) {
                    val slot = session.thePlayer.inventory.searchForItem(0..8) {
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

    private fun searchBlocks(offsetX: Float, offsetY: Float, offsetZ: Float, range: Int, world: WorldClient, expected: Int): List<Vector3i> {
        val possibilities = mutableListOf<Vector3i>()
        val rangeSq = 4.5f * 4.5f
        val blockNear = mutableListOf<EnumFacing>()
        val bb = AxisAlignedBB(offsetX - .3f, offsetY - 1f, offsetZ - .3f, offsetX + .3f, offsetY + .8f, offsetZ + .3f)

        for (x in -range..range) {
            for (z in -range..range) {
                val pos = Vector3i.from(offsetX + x.toDouble(), offsetY - 0.625, offsetZ + z.toDouble())
                if (world.getBlockIdAt(pos) != expected) continue
                else if (pos.distanceSquared(offsetX.toDouble(), offsetY + EntityPlayer.EYE_HEIGHT.toDouble(), offsetZ.toDouble()) > rangeSq) continue
                EnumFacing.values().forEach {
                    val offset = pos.add(it.unitVector)
                    if (world.getBlockIdAt(offset) != expected/*
                        && rayTrace(vectorPosition, pos, it)*/) {
                        blockNear.add(it)
                    }
                }
                if (blockNear.size != 6 && blockNear.isNotEmpty() && bb.intersects(AxisAlignedBB(pos, pos.add(1, 1, 1)))) {
                    possibilities.add(pos)
                }
                blockNear.clear()
            }
        }

        return possibilities
            .sortedBy { it.distanceSquared(offsetX.toDouble(), offsetY-1.0, offsetZ.toDouble()) }
    }

    private fun getFacing(block: Vector3i, world: WorldClient, expected: Int): EnumFacing? {
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
