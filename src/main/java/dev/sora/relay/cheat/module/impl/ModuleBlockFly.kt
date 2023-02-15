package dev.sora.relay.cheat.module.impl

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.math.vector.Vector3i
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import com.nukkitx.protocol.bedrock.packet.PlayerHotbarPacket
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.utils.AxisAlignedBB
import dev.sora.relay.game.utils.constants.EnumFacing
import dev.sora.relay.game.utils.mapping.isBlock
import dev.sora.relay.game.utils.toRotation
import dev.sora.relay.game.utils.toVector3f
import dev.sora.relay.game.world.WorldClient

class ModuleBlockFly : CheatModule("BlockFly") {

    private val swingValue = listValue("Swing", arrayOf("Both", "Client", "Server", "None"), "Server")
    private val adaptiveBlockIdValue = boolValue("AdaptiveBlockId", false)
    private val heldBlockValue = listValue("HeldBlock", arrayOf("Manual", "Auto"), "Manual")
    private val rotationValue = boolValue("Rotation", false)

    private val extendableFacing = arrayOf(EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.NORTH)

    private var lastRotation: Pair<Float, Float>? = null

    @Listen
    fun onTick(event: EventTick) {
        val session = event.session
        if (!switchToBlock()) {
            lastRotation = null
            return
        }

        if (lastRotation != null) {
            session.thePlayer.silentRotation = lastRotation
        }

        val world = session.theWorld
        val airId = if (adaptiveBlockIdValue.get()) {
            world.getBlockIdAt(session.thePlayer.posX.toInt(), session.thePlayer.posY.toInt(),
                session.thePlayer.posZ.toInt())
        } else {
            session.blockMapping.runtime("minecraft:air")
        }
        val possibilities = searchBlocks(session.thePlayer.posX, session.thePlayer.posY - 1.62,
            session.thePlayer.posZ, 1, world, airId)
        val block = possibilities.firstOrNull() ?: return
        val facing = getFacing(block, world, airId) ?: return

//        val id = session.blockMapping.runtime("minecraft:planks[wood_type=oak]")
        val id = session.thePlayer.inventory.hand.blockRuntimeId
        session.netSession.inboundPacket(UpdateBlockPacket().apply {
            blockPosition = block
            runtimeId = id
        })
        world.setBlockIdAt(block.x, block.y, block.z, id)
        session.sendPacket(InventoryTransactionPacket().apply {
            transactionType = TransactionType.ITEM_USE
            actionType = 0
            blockPosition = block.sub(facing.unitVector)
            blockFace = facing.ordinal
            hotbarSlot = session.thePlayer.inventory.heldItemSlot
            itemInHand = session.thePlayer.inventory.hand.let {
                ItemData(it.id, it.damage, it.count, it.tag, it.canPlace, it.canBreak, it.blockingTicks, it.blockRuntimeId, it.extraData, false, 0)
            }
            playerPosition = session.thePlayer.vec3Position
            clickPosition = Vector3f.from(Math.random(), Math.random(), Math.random())
        })
        session.thePlayer.swing(swingValue.get())

        if (rotationValue.get()) {
            lastRotation = toRotation(session.thePlayer.vec3Position, block.sub(facing.unitVector).toVector3f())
            session.thePlayer.silentRotation = lastRotation
        }
    }

    private fun switchToBlock(): Boolean {
        return when(heldBlockValue.get()) {
            "Manual" -> session.thePlayer.inventory.hand.isBlock()
            "Auto" -> {
                if (!session.thePlayer.inventory.hand.isBlock()) {
                    val slot = session.thePlayer.inventory.searchForItem(0..8) {
                        it.blockRuntimeId != 0
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
            else -> false
        }
    }

    private fun searchBlocks(offsetX: Double, offsetY: Double, offsetZ: Double, range: Int, world: WorldClient, expected: Int): List<Vector3i> {
        val possibilities = mutableListOf<Vector3i>()
        val rangeSq = 4.5 * 4.5
        val blockNear = mutableListOf<EnumFacing>()
        val bb = AxisAlignedBB(offsetX - .3, offsetY - 1, offsetZ - .3, offsetX + .3, offsetY + .8, offsetZ + .3)

        for (x in -range..range) {
            for (z in -range..range) {
                val pos = Vector3i.from(offsetX + x, offsetY - 0.625, offsetZ + z)
                if (world.getBlockIdAt(pos) != expected) continue
                else if (pos.distanceSquared(offsetX, offsetY + 1.62, offsetZ) > rangeSq) continue
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
            .sortedBy { it.distanceSquared(offsetX, offsetY-1, offsetZ) }
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
}