package dev.sora.relay

import com.nukkitx.network.raknet.RakNetServerSession
import java.net.InetSocketAddress

interface RakNetRelayListener {
    /**
     * Called when an unconnected client pings the server to retrieve it's status and MOTD.
     *
     * @param address address of client pinging the server
     * @return custom user data sent back to the client
     */
    fun onQuery(address: InetSocketAddress): ByteArray?

    fun onPrepareClientConnection(address: InetSocketAddress): RakNetRelaySessionListener {
        return RakNetRelaySessionListener()
    }

    fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress

    fun onSession(session: RakNetRelaySession) {

    }
}