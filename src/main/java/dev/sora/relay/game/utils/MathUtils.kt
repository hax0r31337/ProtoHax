package dev.sora.relay.game.utils

import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.math.vector.Vector3i
import kotlin.math.hypot
import kotlin.math.roundToInt

data class Rotation(var yaw: Float, var pitch: Float)

fun toRotation(from: Vector3f, to: Vector3f): Rotation {
    val diffX = (to.x - from.x).toDouble()
    val diffY = (to.y - from.y).toDouble()
    val diffZ = (to.z - from.z).toDouble()
    return Rotation(
		(Math.toDegrees(Math.atan2(diffZ, diffX)).toFloat() - 90f),
        ((-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
    )
}

/**
 * Calculate difference between two rotations
 *
 * @param a rotation
 * @param b rotation
 * @author CzechHek
 * @return difference between rotation
 */
fun getRotationDifference(a: Rotation, b: Rotation) =
	hypot(getAngleDifference(a.yaw, b.yaw), a.pitch - b.pitch)

/**
 * Calculate difference between two angle points
 *
 * @param a angle point
 * @param b angle point
 * @author CzechHek
 * @return difference between angle points
 */
fun getAngleDifference(a: Float, b: Float) = ((a - b) % 360f + 540f) % 360f - 180f

fun Vector3i.toVector3f(): Vector3f {
    return Vector3f.from(x.toFloat(), y.toFloat(), z.toFloat())
}

fun Vector3f.toVector3i(): Vector3i {
    return Vector3i.from(x.roundToInt(), y.roundToInt(), z.roundToInt())
}

fun Vector3f.toVector3iFloor(): Vector3i {
	return Vector3i.from(floorX, floorY, floorZ)
}

fun Vector3f.distance(x: Int, y: Int, z: Int): Float {
	return distance(x.toFloat(), y.toFloat(), z.toFloat())
}

fun Vector3f.distance(vector3i: Vector3i): Float {
	return distance(vector3i.x, vector3i.y, vector3i.z)
}
