package dev.sora.relay.game.event

import dev.sora.relay.utils.logError

class EventManager {

    private val handlers = mutableMapOf<Class<out GameEvent>, MutableList<Handler>>()

    fun registerListener(listener: Listener) {
        for (method in listener.javaClass.declaredMethods) {
            if (method.isAnnotationPresent(Listen::class.java)) {
                registerHandler(HandlerMethod(method, listener))
            }
        }
    }

    fun registerHandler(handler: Handler) {
        (handlers[handler.target] ?: mutableListOf<Handler>().also { handlers[handler.target] = it })
            .add(handler)
    }

    fun <T : GameEvent> registerFunction(target: Class<T>, func: (T) -> Unit) {
        registerHandler(HandlerFunction(func, target))
    }

    fun emit(event: GameEvent) {
        for (handler in (handlers[event.javaClass] ?: return)) {
            try {
                handler.invoke(event)
            } catch (t: Throwable) {
                logError("event", t)
            }
        }
    }
}