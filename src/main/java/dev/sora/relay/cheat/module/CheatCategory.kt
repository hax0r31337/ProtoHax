package dev.sora.relay.cheat.module

import dev.sora.relay.cheat.value.NamedChoice

enum class CheatCategory(override val choiceName: String) : NamedChoice {
	COMBAT("Combat"),
	MOVEMENT("Movement"),
	VISUAL("Visual"),
	MISC("Misc")
}
