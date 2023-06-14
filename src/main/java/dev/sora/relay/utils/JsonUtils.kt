package dev.sora.relay.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject

val JsonElement.asJsonObjectOrNull: JsonObject?
	get() = if (this.isJsonObject) this.asJsonObject else null
