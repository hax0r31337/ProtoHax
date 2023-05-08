package dev.sora.relay.session.option

import kotlinx.coroutines.*
import java.util.concurrent.Executors

enum class RelayAsyncOption {

	/**
	 * creates multi-threaded coroutine scope with thread pool dispatcher
	 */
	ENABLED {

		override fun createScope(): CoroutineScope {
			val fixedThreadPoolDispatcher = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()
			return CoroutineScope(fixedThreadPoolDispatcher + SupervisorJob())
		}
	},

	/**
	 * creates single-threaded coroutine scope to disable multi-threading
	 */
	DISABLED {

		@OptIn(DelicateCoroutinesApi::class)
		override fun createScope(): CoroutineScope {
			return CoroutineScope(newSingleThreadContext("RakRelay") + SupervisorJob())
		}
	};

	abstract fun createScope(): CoroutineScope
}
