package dev.sora.relay

import com.nukkitx.network.raknet.RakNetServerSession
import io.netty.bootstrap.Bootstrap
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

interface RakNetRelayListener {
    /**
     * Called when an unconnected client pings the server to retrieve it's status and MOTD.
     *
     * @param address address of client pinging the server
     * @return custom user data sent back to the client
     */
    fun onQuery(address: InetSocketAddress): ByteArray?

    fun onPrepareClientConnection(clientSocket: DatagramChannel): RakNetRelaySessionListener {
        return RakNetRelaySessionListener()
    }

    fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress

    fun onSession(session: RakNetRelaySession) {

    }

    /**
     * @return the bootstrap, if null will use the default one
     */
    fun getBootstrap(): Bootstrap? = null
}