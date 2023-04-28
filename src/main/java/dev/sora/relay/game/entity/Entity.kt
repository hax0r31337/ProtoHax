package dev.sora.relay.game.entity

import dev.sora.relay.game.inventory.EntityInventory
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.AttributeData
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData
import org.cloudburstmc.protocol.bedrock.packet.*
import kotlin.math.sqrt

abstract class Entity(open val entityId: Long) {

    open var posX = 0f
    open var posY = 0f
    open var posZ = 0f

    open var prevPosX = 0f
    open var prevPosY = 0f
    open var prevPosZ = 0f

    open var rotationYaw = 0f
    open var rotationPitch = 0f
    open var rotationYawHead = 0f

    open var motionX = 0f
    open var motionY = 0f
    open var motionZ = 0f

    open var tickExists = 0L
        protected set

	var rideEntity: Long? = null

    open val attributes = mutableMapOf<String, AttributeData>()
    open val metadata = EntityDataMap()

    open val inventory = EntityInventory(entityId)

    val vec3Position: Vector3f
        get() = Vector3f.from(posX, posY, posZ)

    val vec3Rotation: Vector3f
        get() = Vector3f.from(rotationPitch, rotationYaw, rotationYawHead)

    open fun move(x: Float, y: Float, z: Float) {
        this.prevPosX = this.posX
        this.prevPosY = this.posY
        this.prevPosZ = this.posZ
        this.posX = x
        this.posY = y
        this.posZ = z
        this.motionX = x - prevPosX
        this.motionY = y - prevPosY
        this.motionZ = z - prevPosZ
    }

    open fun move(position: Vector3f) {
        move(position.x, position.y, position.z)
    }

    open fun rotate(yaw: Float, pitch: Float) {
        this.rotationYaw = yaw
        this.rotationPitch = pitch
    }

    open fun rotate(yaw: Float, pitch: Float, headYaw: Float) {
        rotate(yaw, pitch)
        rotationYawHead = headYaw
    }

    open fun rotate(rotation: Vector2f) {
        rotate(rotation.y, rotation.x)
    }

    open fun rotate(rotation: Vector3f) {
        rotate(rotation.y, rotation.x, rotation.z)
    }

    fun distanceSq(x: Float, y: Float, z: Float): Float {
        val dx = posX - x
        val dy = posY - y
        val dz = posZ - z
        return dx * dx + dy * dy + dz * dz
    }

    fun distanceSq(entity: Entity)
            = distanceSq(entity.posX, entity.posY, entity.posZ)

    fun distance(x: Float, y: Float, z: Float)
        = sqrt(distanceSq(x, y, z))

    fun distance(entity: Entity)
        = distance(entity.posX, entity.posY, entity.posZ)

    open fun onPacket(packet: BedrockPacket) {
        if (packet is MoveEntityAbsolutePacket && packet.runtimeEntityId == entityId) {
            move(packet.position)
            rotate(packet.rotation)
            tickExists++
        } else if (packet is MoveEntityDeltaPacket && packet.runtimeEntityId == entityId) {
            move(posX + if (packet.flags.contains(MoveEntityDeltaPacket.Flag.HAS_X)) packet.x else 0f,
                posY + if (packet.flags.contains(MoveEntityDeltaPacket.Flag.HAS_Y)) packet.y else 0f,
                posZ + if (packet.flags.contains(MoveEntityDeltaPacket.Flag.HAS_Z)) packet.z else 0f)
            rotate(rotationYaw + if (packet.flags.contains(MoveEntityDeltaPacket.Flag.HAS_YAW)) packet.yaw else 0f,
                rotationPitch + if (packet.flags.contains(MoveEntityDeltaPacket.Flag.HAS_PITCH)) packet.pitch else 0f,
                rotationYawHead + if (packet.flags.contains(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW)) packet.headYaw else 0f)
            tickExists++
        } else if (packet is SetEntityDataPacket && packet.runtimeEntityId == entityId) {
            handleSetData(packet.metadata)
        } else if (packet is UpdateAttributesPacket && packet.runtimeEntityId == entityId) {
            handleSetAttribute(packet.attributes)
        } else if (packet is SetEntityLinkPacket) {
			when(packet.entityLink.type) {
				EntityLinkData.Type.RIDER -> if (packet.entityLink.from == entityId) rideEntity = packet.entityLink.to
				EntityLinkData.Type.REMOVE -> if (packet.entityLink.from == entityId) rideEntity = null
				EntityLinkData.Type.PASSENGER -> if (packet.entityLink.to == entityId) rideEntity = packet.entityLink.from
				else -> {}
			}
		} else {
            inventory.handlePacket(packet)
        }
    }

    internal open fun handleSetData(map: EntityDataMap) {
        map.forEach { (key, value) ->
            metadata[key] = value
        }
    }

    internal open fun handleSetAttribute(attributeList: List<AttributeData>) {
        attributeList.forEach {
            attributes[it.name] = it
        }
    }

    open fun reset() {
//        attributeList.clear()
        metadata.clear()
    }
}
