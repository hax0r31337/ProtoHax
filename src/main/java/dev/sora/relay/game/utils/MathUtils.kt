package dev.sora.relay.game.utils

import com.nukkitx.math.vector.Vector3f
import com.nukkitx.math.vector.Vector3i
import kotlin.math.roundToInt

fun toRotation(from: Vector3f, to: Vector3f): Pair<Float, Float> {
    val diffX = (to.x - from.x).toDouble()
    val diffY = (to.y - from.y).toDouble()
    val diffZ = (to.z - from.z).toDouble()
    return Pair(
        ((-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)))).toFloat()),
        (Math.toDegrees(Math.atan2(diffZ, diffX)).toFloat() - 90f)
    )
}

fun Vector3i.toVector3f(): Vector3f {
    return Vector3f.from(x.toFloat(), y.toFloat(), z.toFloat())
}

fun Vector3f.toVector3i(): Vector3i {
    return Vector3i.from(x.roundToInt(), y.roundToInt(), z.roundToInt())
}