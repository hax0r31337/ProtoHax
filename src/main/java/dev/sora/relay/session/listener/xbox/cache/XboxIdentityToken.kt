package dev.sora.relay.session.listener.xbox.cache

import java.time.Instant

data class XboxIdentityToken(val token: String, val notAfter: Long) {

    val expired: Boolean
        get() = notAfter < Instant.now().epochSecond
}
