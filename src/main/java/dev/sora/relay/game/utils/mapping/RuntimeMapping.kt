package dev.sora.relay.game.utils.mapping


private const val FALLBACK_GAME = "minecraft:unknown"
private const val FALLBACK_RUNTIME = 0

interface RuntimeMapping {

    fun runtime(game: String): Int

    fun game(runtime: Int): String
}

class EmptyRuntimeMapping(private val fallbackGame: String = FALLBACK_GAME,
                          private val fallbackRuntime: Int = FALLBACK_RUNTIME) : RuntimeMapping {

    override fun game(runtime: Int) = fallbackGame

    override fun runtime(game: String) = fallbackRuntime
}

open class RuntimeMappingImpl : RuntimeMapping {

    private val runtimeToGameMap: Map<Int, String>
    private val gameToRuntimeMap: Map<String, Int>

    constructor(data: List<Pair<String, Int>>) {
        val runtimeMap = mutableMapOf<Int, String>()
        val itemMap = mutableMapOf<String, Int>()
        data.forEach {
            itemMap[it.first] = it.second
            runtimeMap[it.second] = it.first
        }
        runtimeToGameMap = runtimeMap
        gameToRuntimeMap = itemMap
    }

    constructor(runtimeToGameMap: Map<Int, String>,
                gameToRuntimeMap: Map<String, Int>) {
        this.runtimeToGameMap = runtimeToGameMap
        this.gameToRuntimeMap = gameToRuntimeMap
    }

    override fun runtime(game: String): Int {
        return gameToRuntimeMap[game] ?: FALLBACK_RUNTIME
    }

    override fun game(runtime: Int): String {
        return runtimeToGameMap[runtime] ?: FALLBACK_GAME
    }
}