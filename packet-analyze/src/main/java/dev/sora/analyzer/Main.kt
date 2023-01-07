package dev.sora.analyzer

import com.google.gson.JsonParser
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.BedrockPacketCodec
import dev.sora.relay.RakNetRelay
import dev.sora.relay.RakNetRelayListener
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.HttpUtils
import java.io.File
import java.lang.reflect.Modifier
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    val verNum = if (args.size > 2) args[2] else "560"
    val relay = RakNetRelay(InetSocketAddress("0.0.0.0", 19132), packetCodec = getPacketCodec(verNum))
    relay.listener = object : RakNetRelayListener {
        override fun onQuery(address: InetSocketAddress) =
            "MCPE;RakNet Relay;557;1.19.20;0;10;${relay.server.guid};Bedrock level;Survival;1;19132;19132;".toByteArray()

        override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
            return InetSocketAddress(args[0], args[1].toInt())
        }

        override fun onSession(session: RakNetRelaySession) {
            if (File(".ms_refresh_token").exists()) {
                session.listener.childListener.add(RakNetRelaySessionListenerMicrosoft(getMSAccessToken(), session))
            }
            session.listener.childListener.add(object : RakNetRelaySessionListener.PacketListener {
                override fun onPacketInbound(packet: BedrockPacket): Boolean {
                    println("in < $packet")
                    return true
                }

                override fun onPacketOutbound(packet: BedrockPacket): Boolean {
                    println("out > $packet")
                    return true
                }
            })
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

private fun getPacketCodec(codecVersion: String): BedrockPacketCodec {
    val klass = Class.forName("com.nukkitx.protocol.bedrock.$codecVersion.Bedrock_$codecVersion")
    klass.fields.forEach {
        if(Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers)) {
            val value = it.get(null)
            if(value is BedrockPacketCodec) {
                return value
            }
        }
    }
    throw IllegalStateException("failed")
}