package dev.sora.relay.utils.timing

open class MillisecondTimer {

    protected var time = System.currentTimeMillis()

    fun reset() {
        time = System.currentTimeMillis()
    }

    fun getTimePassed(): Long {
        return System.currentTimeMillis() - time
    }

    fun hasTimePassed(time: Int) = hasTimePassed(time.toLong())

    fun hasTimePassed(time: Long): Boolean {
        return getTimePassed() >= time
    }
}
