package dev.sora.relay.utils

import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class CipherPair(val secretKey: SecretKey) {

    val encryptionCipher: Cipher
    val decryptionCipher: Cipher
    val sentEncryptedPacketCount = AtomicLong()

    init {
        val iv = ByteArray(16)
        val transformation = "AES/CTR/NoPadding"
        System.arraycopy(secretKey.encoded, 0, iv, 0, 12)
        iv[15] = 2
        encryptionCipher = Cipher.getInstance(transformation).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        }
        decryptionCipher = Cipher.getInstance(transformation).apply {
            init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        }
    }
}