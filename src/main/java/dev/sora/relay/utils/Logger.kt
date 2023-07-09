package dev.sora.relay.utils

import dev.sora.relay.game.GameSession
import io.netty.util.internal.logging.InternalLoggerFactory


private val logger = InternalLoggerFactory.getInstance(GameSession::class.java)

fun logInfo(vararg msg: Any?) {
    logger.info(msg.joinToString(" "))
}

fun logWarn(vararg msg: Any?) {
    logger.warn(msg.joinToString(" "))
}

fun logError(vararg msg: Any?) {
    logger.error(msg.joinToString(" "))
}

fun logError(msg: String, t: Throwable) {
    logger.error(msg, t)
}
