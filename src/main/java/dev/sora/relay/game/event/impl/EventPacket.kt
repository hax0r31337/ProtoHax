package dev.sora.relay.game.event.impl

import com.nukkitx.protocol.bedrock.BedrockPacket
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.GameEventCancelable

class EventPacketInbound(session: GameSession, val packet: BedrockPacket) : GameEventCancelable(session)

class EventPacketOutbound(session: GameSession, val packet: BedrockPacket) : GameEventCancelable(session)