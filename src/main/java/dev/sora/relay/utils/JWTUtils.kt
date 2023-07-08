package dev.sora.relay.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.sora.relay.cheat.config.AbstractConfigManager
import org.jose4j.jws.EcdsaUsingShaAlgorithm
import java.security.KeyPair
import java.security.Signature
import java.util.*

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
	val signatureBytes = EcdsaUsingShaAlgorithm.convertDerToConcatenated(signature.sign(), 48)
	return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes)
}
