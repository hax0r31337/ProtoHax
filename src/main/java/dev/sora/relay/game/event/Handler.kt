package dev.sora.relay.game.event

import java.lang.reflect.Method

interface Handler {

    val target: Class<out GameEvent>

    fun invoke(event: GameEvent)
}

class HandlerFunction<T : GameEvent>(private val func: (T) -> Unit, override val target: Class<T>) : Handler {

    override fun invoke(event: GameEvent) {
        func(event as T)
    }
}

class HandlerMethod(private val method: Method, private val listener: Listener) : Handler {
    init {
        if (!method.isAccessible) {
            method.isAccessible = true
        }
    }

    override val target = method.parameterTypes[0] as Class<out GameEvent>

    override fun invoke(event: GameEvent) {
        if (listener.listen()) {
            try {
                method.invoke(listener, event)
            } catch (t: Throwable) {
                Exception("An error occurred while handling the event: ", t).printStackTrace()
            }
        }
    }
}

interface Listener {
    fun listen(): Boolean
}

annotation class Listen