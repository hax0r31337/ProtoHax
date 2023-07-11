package dev.sora.relay.utils.timing

class ClickTimer : MillisecondTimer() {

    private var delay: Long = 0

    fun canClick(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - time >= delay
    }

    fun update(minCPS: Int, maxCPS: Int) {
        delay = ((Math.random() * (1000 / minCPS - 1000 / maxCPS + 1)) + 1000 / maxCPS).toLong()
        time = System.currentTimeMillis()
    }
}
