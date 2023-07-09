package dev.sora.relay

import dev.sora.netty.JdkLogger
import io.netty.util.internal.logging.InternalLogger
import io.netty.util.internal.logging.InternalLoggerFactory
import java.util.logging.Level
import java.util.logging.Logger

class LoggerFactory : InternalLoggerFactory() {
    override fun newInstance(name: String): InternalLogger {
        return JdkLogger(Logger.getLogger(name).apply {
            level = Level.FINER
        })
    }
}
