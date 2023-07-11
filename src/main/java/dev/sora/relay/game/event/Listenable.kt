package dev.sora.relay.game.event

interface Listenable {

	val eventManager: EventManager

    /**
     * whether handle events or not
     */
	val handleEvents: Boolean
		get() = true

}

@Suppress("unchecked_cast")
inline fun <reified T : GameEvent> Listenable.handle(noinline handler: Handler<T>) {
	eventManager.register(EventHook(T::class.java, handler, this) as EventHook<in GameEvent>)
}
