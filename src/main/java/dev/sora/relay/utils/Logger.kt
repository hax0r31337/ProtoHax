package dev.sora.relay.utils

import dev.sora.relay.game.GameSession
import io.netty.util.internal.logging.InternalLoggerFactory


private val logger = InternalLoggerFactory.getInstance(GameSession::class.java)

fun logInfo(msg: String) {
    logger.info(msg)
}

fun logWarn(msg: String) {
    logger.warn(msg)
}

fun logError(msg: String) {
    logger.error(msg)
}

fun logError(msg: String, t: Throwable) {
    logger.error(msg, t)
}
