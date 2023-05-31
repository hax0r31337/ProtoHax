package dev.sora.relay.utils

import java.util.*

fun getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun ByteArray.toHexString(separator: String = ""): String {
    return this.joinToString(separator) { String.format("%02x", it) }
}

fun base64Decode(b64: String): ByteArray {
    return Base64.getDecoder().decode(b64.replace('-', '+').replace('_', '/'))
}

private val controlCodesMinecraftString = arrayOf('k', 'l', 'm', 'n', 'o', 'r')

/**
 * counts color coverage of a Minecraft colored (§) string
 */
fun analyzeColorCoverage(str: String): Map<Char, Int> {
	val stringParts = str.split('§')
	val colorMap = mutableMapOf<Char, Int>()

	var currentColor = 'f'

	stringParts.forEachIndexed { i, s ->
		val p = if (i == 0) {
			s
		} else {
			val control = s[0]
			if (!controlCodesMinecraftString.contains(control)) {
				currentColor = control
			} else if (control == 'r') { // resets color
				currentColor = 'f'
			}
			s.substring(1)
		}

		colorMap[currentColor] = (colorMap[currentColor] ?: 0) + p.length
	}

	return colorMap
}
