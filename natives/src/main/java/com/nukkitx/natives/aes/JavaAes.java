package com.nukkitx.natives.aes;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.util.function.Supplier;

public class JavaAes implements Aes {
    public static final AesFactory FACTORY = JavaAes::new;
    public static final Supplier<AesFactory> SUPPLIER = () -> FACTORY;

    private final Cipher cipher;

    private JavaAes(boolean encrypt, SecretKey key, IvParameterSpec iv) {
        try {
            this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            int mode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
            this.cipher.init(mode, key, iv);
        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalArgumentException("Invalid key given");
        } catch (GeneralSecurityException e) {
            throw new AssertionError("Expected AES to be available");
        }
    }

    @Override
    public void cipher(ByteBuffer input, ByteBuffer output) throws ShortBufferException {
        this.cipher.update(input, output);
    }

    @Override
    public void free() {

    }
}
