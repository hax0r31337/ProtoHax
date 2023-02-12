package dev.sora.relay.cheat.module.impl

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.math.vector.Vector3i
import com.nukkitx.protocol.bedrock.data.inventory.ItemData
import com.nukkitx.protocol.bedrock.data.inventory.TransactionType
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.EventTick
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.utils.AxisAlignedBB
import dev.sora.relay.game.utils.constants.EnumFacing
import dev.sora.relay.game.world.WorldClient
import dev.sora.relay.utils.logInfo

class ModuleBlockFly : CheatModule("BlockFly") {

    private val swingValue = listValue("Swing", arrayOf("Both", "Client", "Server", "None"), "Server")
    private val heldBlockValue = boolValue("HeldBlock", true)

    private val extendableFacing = arrayOf(EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.NORTH)

    @Listen
    fun onTick(event: EventTick) {
        val session = event.session
        val heldBlockId = session.thePlayer.inventory.hand.blockRuntimeId
        if (heldBlockValue.get() && heldBlockId == 0) return

        val world = session.theWorld
        val airId = session.blockMapping.runtime("minecraft:air")
        val possibilities = searchBlocks(session.thePlayer.posX, session.thePlayer.posY - 1.62,
            session.thePlayer.posZ, 1, world, airId)
        val block = possibilities.firstOrNull() ?: return
        val facing = getFacing(block, world, airId) ?: return

//        val id = session.blockMapping.runtime("minecraft:planks[wood_type=oak]")
        session.netSession.inboundPacket(UpdateBlockPacket().apply {
            blockPosition = block
            runtimeId = heldBlockId
        })
        world.setBlockIdAt(block.x, block.y, block.z, heldBlockId)
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
        session.thePlayer.swing(when(swingValue.get()) {
            "Both" -> EntityPlayerSP.SwingMode.BOTH
            "Client" -> EntityPlayerSP.SwingMode.CLIENTSIDE
            "Server" -> EntityPlayerSP.SwingMode.SERVERSIDE
            else -> EntityPlayerSP.SwingMode.NONE
        })
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