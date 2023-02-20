package dev.sora.relay.game.event

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.inventory.AbstractInventory
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

abstract class GameEvent(val session: GameSession)

abstract class GameEventCancellable(session: GameSession) : GameEvent(session) {

    private var canceled = false

    open fun cancel() {
        canceled = true
    }

    open fun isCanceled() = canceled

}

class EventTick(session: GameSession) : GameEvent(session)

/**
 * @param reason BedrockDisconnectReasons
 */
class EventDisconnect(session: GameSession, val client: Boolean, val reason: String) : GameEvent(session)

class EventPacketInbound(session: GameSession, val packet: BedrockPacket) : GameEventCancellable(session)

class EventPacketOutbound(session: GameSession, val packet: BedrockPacket) : GameEventCancellable(session)

/**
 * the container just "initialized", the content not received
 */
class EventContainerOpen(session: GameSession, val container: AbstractInventory) : GameEvent(session)

class EventContainerClose(session: GameSession, val container: AbstractInventory) : GameEvent(session)