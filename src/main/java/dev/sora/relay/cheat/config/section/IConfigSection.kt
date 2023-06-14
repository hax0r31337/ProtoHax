package dev.sora.relay.cheat.config.section

import com.google.gson.JsonElement

interface IConfigSection {

	val sectionName: String

	/**
	 * the json element from root config json with the key of [sectionName],
	 * if the key do not exists in root config json, null will be passed.
	 */
	fun load(element: JsonElement?)

	/**
	 * null value will be ignored
	 */
	fun save(): JsonElement?
}
