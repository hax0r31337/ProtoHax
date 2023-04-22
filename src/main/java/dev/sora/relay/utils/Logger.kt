package dev.sora.relay.utils

import dev.sora.relay.game.GameSession
import io.netty.util.internal.logging.InternalLoggerFactory


private val logger = InternalLoggerFactory.getInstance(GameSession::class.java)

fun logInfo(vararg msg: String) {
    logger.info(msg.joinToString(" "))
}

fun logWarn(vararg msg: String) {
    logger.warn(msg.joinToString(" "))
}

fun logError(vararg msg: String) {
    logger.error(msg.joinToString(" "))
}

fun logError(msg: String, t: Throwable) {
    logger.error(msg, t)
}
