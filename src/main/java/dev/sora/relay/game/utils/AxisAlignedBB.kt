package dev.sora.relay.game.utils

import com.nukkitx.math.vector.Vector3d
import com.nukkitx.math.vector.Vector3i
import kotlin.math.floor

data class AxisAlignedBB(var minX: Double, var minY: Double, var minZ: Double,
                         var maxX: Double, var maxY: Double, var maxZ: Double) {

    constructor(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int) : this(minX.toDouble(), minY.toDouble(), minZ.toDouble(), maxX.toDouble(), maxY.toDouble(), maxZ.toDouble())

    constructor(vec: Vector3d, vec1: Vector3d) : this(vec.x, vec.y, vec.z, vec1.x, vec1.y, vec1.z)

    constructor(vec: Vector3i, vec1: Vector3i) : this(vec.x, vec.y, vec.z, vec1.x, vec1.y, vec1.z)

    fun floor() {
        this.minX = floor(this.minX)
        this.minY = floor(this.minY)
        this.minZ = floor(this.minZ)
        this.maxX = floor(this.maxX)
        this.maxY = floor(this.maxY)
        this.maxZ = floor(this.maxZ)
    }

    fun addCoord(x: Double, y: Double, z: Double) {
        if (x < 0) this.minX += x
        else this.maxX += x

        if (y < 0) this.minY += y
        else this.maxY += y

        if (z < 0) this.minZ += z
        else this.maxZ += z
    }

    fun contract(x: Double, y: Double, z: Double) {
        this.minX += x
        this.minY += y
        this.minZ += z
        this.maxX -= x
        this.maxY -= y
        this.maxZ -= z
    }

    fun expand(x: Double, y: Double, z: Double) {
        this.minX -= x
        this.minY -= y
        this.minZ -= z
        this.maxX += x
        this.maxY += y
        this.maxZ += z
    }

    fun offset(x: Double, y: Double, z: Double) {
        this.minX += x
        this.minY += y
        this.minZ += z
        this.maxX += x
        this.maxY += y
        this.maxZ += z
    }

    fun intersects(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return this.minX < maxX && this.maxX > minX && this.minY < maxY &&
                this.maxY > minY && this.minZ < maxZ && this.maxZ > minZ
    }

    fun intersects(other: AxisAlignedBB): Boolean {
        return this.minX < other.maxX && this.maxX > other.minX && this.minY < other.maxY &&
                this.maxY > other.minY && this.minZ < other.maxZ && this.maxZ > other.minZ
    }

    fun intersectsXZ(other: AxisAlignedBB): Boolean {
        return this.minX < other.maxX && this.maxX > other.minX && this.minZ < other.maxZ && this.maxZ > other.minZ
    }

    fun intersectsY(other: AxisAlignedBB): Boolean {
        return this.minY < other.maxY && this.maxY > other.minY
    }
}