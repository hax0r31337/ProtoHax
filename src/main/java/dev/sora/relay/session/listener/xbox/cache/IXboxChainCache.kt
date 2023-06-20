package dev.sora.relay.session.listener.xbox.cache

import com.nimbusds.jwt.SignedJWT
import dev.sora.relay.session.listener.xbox.XboxDeviceInfo
import java.security.KeyPair

interface IXboxChainCache {

	/**
	 * identifier for the account which used to cache
	 */
	val identifier: String

	fun cache(device: XboxDeviceInfo, expires: Long, body: List<SignedJWT>, keyPair: KeyPair)

	fun checkCache(device: XboxDeviceInfo): Pair<List<SignedJWT>, KeyPair>?
}
