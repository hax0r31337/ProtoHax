package dev.sora.relay.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.config.AbstractConfigManager
import java.security.KeyPair
import java.security.Signature
import java.security.SignatureException
import java.util.Base64
import kotlin.math.max
import kotlin.math.min

fun jwtPayload(jwt: String): JsonObject? {
	val parts = jwt.split('.')
	if (parts.size != 3) {
		// not a jwt string
		return null
	}

	return JsonParser.parseReader(base64Decode(parts[1]).inputStream().reader(Charsets.UTF_8))
		.asJsonObjectOrNull
}

fun signJWT(payload: String, keyPair: KeyPair, base64Encoded: Boolean = false): String {
	val headerJson = JsonObject().apply {
		addProperty("alg", "ES384")
		addProperty("x5u", Base64.getEncoder().withoutPadding().encodeToString(keyPair.public.encoded))
	}
	val header = Base64.getUrlEncoder().withoutPadding().encodeToString(AbstractConfigManager.DEFAULT_GSON.toJson(headerJson).toByteArray(Charsets.UTF_8))
	val encodedPayload = if (base64Encoded) payload else Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(Charsets.UTF_8))
	val sign = signBytes("$header.$encodedPayload".toByteArray(Charsets.UTF_8), keyPair)
	return "$header.$encodedPayload.$sign"
}

private fun signBytes(dataToSign: ByteArray, keyPair: KeyPair): String {
	val signature = Signature.getInstance("SHA384withECDSA")
	signature.initSign(keyPair.private)
	signature.update(dataToSign)
	val signatureBytes = derToJose(signature.sign())
	return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes)
}

private fun derToJose(derSignature: ByteArray): ByteArray {
	val ecNumberSize = 48
	val derEncoded = derSignature[0] == 0x30.toByte() && derSignature.size != ecNumberSize * 2

	if (!derEncoded) {
		throw SignatureException("Invalid DER signature format.")
	}

	val joseSignature = ByteArray(ecNumberSize * 2)

	var offset = 1
	if (derSignature[1] == 0x81.toByte()) {
		offset++
	}

	val encodedLength = derSignature[offset++].toInt() and 0xff
	if (encodedLength != derSignature.size - offset) {
		throw SignatureException("Invalid DER signature format.")
	}

	offset++

	val rLength = derSignature[offset++].toInt()
	if (rLength > ecNumberSize + 1) {
		throw SignatureException("Invalid DER signature format.")
	}
	val rPadding = ecNumberSize - rLength
	val rStart = offset + max(-rPadding, 0)
	val rEnd = rLength + min(rPadding, 0)
	derSignature.copyInto(joseSignature, max(rPadding, 0), rStart, rStart + rEnd)

	offset += rLength + 1

	val sLength = derSignature[offset++].toInt()
	if (sLength > ecNumberSize + 1) {
		throw SignatureException("Invalid DER signature format.")
	}
	val sPadding = ecNumberSize - sLength
	val sStart = offset + max(-sPadding, 0)
	val sEnd = sLength + min(sPadding, 0)
	derSignature.copyInto(joseSignature, ecNumberSize + max(sPadding, 0), sStart, sStart + sEnd)

	return joseSignature
}
