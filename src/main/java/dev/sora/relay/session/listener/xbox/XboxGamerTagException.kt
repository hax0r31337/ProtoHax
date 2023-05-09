package dev.sora.relay.session.listener.xbox

/**
 * thown whilst no xbox gamer tag found on account
 */
class XboxGamerTagException(val sisuStartUrl: String)
	: IllegalStateException("Have you registered a Xbox GamerTag? You can register it here: $sisuStartUrl")
