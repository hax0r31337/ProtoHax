package dev.sora.relay.game.entity

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket
import kotlin.math.sqrt

abstract class Entity(open val entityId: Long) {

    open var posX = 0.0
    open var posY = 0.0
    open var posZ = 0.0

    open var prevPosX = 0.0
    open var prevPosY = 0.0
    open var prevPosZ = 0.0

    open var rotationYaw = 0f
    open var rotationPitch = 0f

    open var motionX = 0.0
    open var motionY = 0.0
    open var motionZ = 0.0

    var tickExists = 0L

//    val attributeList = mutableListOf<AttributeData>()
//    val metadataList = EntityDataMap()

    // TODO: inventory

    open fun move(x: Double, y: Double, z: Double) {
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
        move(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
    }

    open fun rotate(yaw: Float, pitch: Float) {
        this.rotationYaw = yaw
        this.rotationPitch = pitch
    }

    open fun rotate(rotation: Vector3f) {
        rotate(rotation.y, rotation.x)
    }

    fun distanceSq(x: Double, y: Double, z: Double): Double {
        val dx = posX - x
        val dy = posY - y
        val dz = posZ - z
        return dx * dx + dy * dy + dz * dz
    }

    fun distanceSq(entity: Entity)
            = distanceSq(entity.posX, entity.posY, entity.posZ)

    fun distance(x: Double, y: Double, z: Double)
        = sqrt(distanceSq(x, y, z))

    fun distance(entity: Entity)
        = distance(entity.posX, entity.posY, entity.posZ)

    fun vec3Position(): Vector3f {
        return Vector3f.from(posX, posY, posZ)
    }

    open fun onPacket(packet: BedrockPacket) {
        if (packet is MoveEntityAbsolutePacket && packet.runtimeEntityId == entityId) {
            move(packet.position)
            tickExists++
        } /* else if (packet is MoveEntityDeltaPacket && packet.runtimeEntityId == entityId) {
            // TODO
        } */
    }

    open fun reset() {
//        attributeList.clear()
//        metadataList.clear()
    }
}