package dev.sora.relay

import dev.sora.relay.session.MinecraftRelaySession
import java.net.InetSocketAddress

interface MinecraftRelayListener {

    fun onSessionCreation(session: MinecraftRelaySession): InetSocketAddress
}