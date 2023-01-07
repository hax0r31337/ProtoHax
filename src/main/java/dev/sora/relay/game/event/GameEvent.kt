package dev.sora.relay.game.event

import dev.sora.relay.game.GameSession

abstract class GameEvent(val session: GameSession)

abstract class GameEventCancelable(session: GameSession) : GameEvent(session) {

    private var canceled = false

    open fun cancel() {
        canceled = true
    }

    open fun isCanceled() = canceled

}