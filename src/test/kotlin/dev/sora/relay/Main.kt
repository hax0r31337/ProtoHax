package dev.sora.relay

import com.google.gson.JsonParser
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.v557.Bedrock_v557
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.command.impl.CommandToggle
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.game.GameSession
import dev.sora.relay.utils.HttpUtils
import io.netty.util.internal.logging.InternalLoggerFactory
import java.io.File
import java.net.InetSocketAddress


fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(LoggerFactory())
    val gameSession = craftSession()

    val relay = RakNetRelay(InetSocketAddress("0.0.0.0", 19132), packetCodec = Bedrock_v557.V557_CODEC)
    relay.listener = object : RakNetRelayListener {
        override fun onQuery(address: InetSocketAddress) =
            "MCPE;RakNet Relay;557;1.19.20;0;10;${relay.server.guid};Bedrock level;Survival;1;19132;19132;".toByteArray()

        override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
            return InetSocketAddress("127.0.0.1", 19136)
//            return InetSocketAddress("101.67.57.190", 19114)
        }

        override fun onSession(session: RakNetRelaySession) {
            session.listener.childListener.add(gameSession)
            gameSession.netSession = session
//            session.listener.childListener.add(RakNetRelaySessionListenerMicrosoft(getMSAccessToken(), session))
        }
    }
    relay.bind()
    println("bind")
    Thread.sleep(Long.MAX_VALUE)
}

private fun getMSAccessToken(): String {
    val file = File(".ms_refresh_token")
    val body = JsonParser.parseReader(
        HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
        "client_id=00000000441cc96b&scope=service::user.auth.xboxlive.com::MBI_SSL&grant_type=refresh_token&redirect_uri=https://login.live.com/oauth20_desktop.srf&refresh_token=${file.readText(Charsets.UTF_8)}",
        mapOf("Content-Type" to "application/x-www-form-urlencoded")).inputStream.reader(Charsets.UTF_8)).asJsonObject
    file.writeText(body.get("refresh_token").asString)
    return body.get("access_token").asString
}

private fun craftSession() : GameSession {
    val session = GameSession()

    val moduleManager = ModuleManager(session)
    moduleManager.init()

    val commandManager = CommandManager(session)
    commandManager.registerCommand(CommandToggle(moduleManager))

    session.eventManager.registerListener(commandManager)

    return session
}