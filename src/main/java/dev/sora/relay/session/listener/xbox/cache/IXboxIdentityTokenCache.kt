package dev.sora.relay.session.listener.xbox.cache

import dev.sora.relay.session.listener.xbox.XboxDeviceInfo

interface IXboxIdentityTokenCache {

	/**
	 * identifier for the account which used to cache
	 */
	val identifier: String

	fun cache(device: XboxDeviceInfo, token: XboxIdentityToken)

	fun checkCache(device: XboxDeviceInfo): XboxIdentityToken?
}
