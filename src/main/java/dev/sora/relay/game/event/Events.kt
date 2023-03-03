package dev.sora.relay.game.event

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.inventory.AbstractInventory
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket

abstract class GameEvent(val session: GameSession, val friendlyName: String)

abstract class GameEventCancellable(session: GameSession, friendlyName: String) : GameEvent(session, friendlyName) {

    private var canceled = false

    open fun cancel() {
        canceled = true
    }

    open fun isCanceled() = canceled

}

class EventTick(session: GameSession) : GameEvent(session, "tick")

/**
 * @param reason BedrockDisconnectReasons
 */
class EventDisconnect(session: GameSession, val client: Boolean, val reason: String) : GameEvent(session, "disconnect")

class EventPacketInbound(session: GameSession, val packet: BedrockPacket) : GameEventCancellable(session, "packet_inbound")

class EventPacketOutbound(session: GameSession, val packet: BedrockPacket) : GameEventCancellable(session, "packet_outbound")

/**
 * the container just "initialized", the content not received
 */
class EventContainerOpen(session: GameSession, val container: AbstractInventory) : GameEvent(session, "container_open")

class EventContainerClose(session: GameSession, val container: AbstractInventory) : GameEvent(session, "container_close")