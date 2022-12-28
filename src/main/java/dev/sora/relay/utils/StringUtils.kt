package dev.sora.relay.utils

fun getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun ByteArray.toHexString(separator: String = ""): String {
    return this.joinToString(separator) { String.format("%02x", it) }
}