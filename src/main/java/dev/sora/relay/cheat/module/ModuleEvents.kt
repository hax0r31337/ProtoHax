package dev.sora.relay.cheat.module

import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.GameEventCancellable

class EventModuleToggle(session: GameSession, val module: CheatModule, val targetState: Boolean) : GameEventCancellable(session, "module_toggle")
