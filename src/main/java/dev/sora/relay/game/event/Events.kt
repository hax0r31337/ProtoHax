package dev.sora.relay.game.event

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.inventory.AbstractInventory
import dev.sora.relay.game.world.chunk.Chunk
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

class EventPostTick(session: GameSession) : GameEvent(session, "post_tick")

/**
 * @param reason BedrockDisconnectReasons
 */
class EventDisconnect(session: GameSession, val client: Boolean, val reason: String) : GameEvent(session, "disconnect")

class EventPacketInbound(session: GameSession, val packet: BedrockPacket) : GameEventCancellable(session, "packet_inbound")

class EventPacketOutbound(session: GameSession, val packet: BedrockPacket) : GameEventCancellable(session, "packet_outbound")

class EventPacketPostOutbound(session: GameSession, val packet: BedrockPacket) : GameEvent(session, "packet_post_outbound")

/**
 * the container just "initialized", the content not received
 */
class EventContainerOpen(session: GameSession, val container: AbstractInventory) : GameEvent(session, "container_open")

class EventContainerClose(session: GameSession, val container: AbstractInventory) : GameEvent(session, "container_close")

/**
 * triggered on LevelChunkPacket,
 * but be aware if the chunk have separate subchunks deliver, the subchunk will not be loaded on the event call
 */
class EventChunkLoad(session: GameSession, val chunk: Chunk) : GameEvent(session, "chunk_load")

class EventChunkUnload(session: GameSession, val chunk: Chunk) : GameEvent(session, "chunk_unload")

class EventDimensionChange(session: GameSession, val dimension: Int) : GameEvent(session, "dimension_change")

class EventEntitySpawn(session: GameSession, val entity: Entity) : GameEvent(session, "entity_spawn")

class EventEntityDespawn(session: GameSession, val entity: Entity) : GameEvent(session, "entity_despawn")
