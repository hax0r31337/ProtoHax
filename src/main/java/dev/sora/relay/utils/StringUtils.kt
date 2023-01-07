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