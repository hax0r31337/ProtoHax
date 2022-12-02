package com.nukkitx.natives.sha256;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

public class JavaSha256 implements Sha256 {
    public static final Supplier<Sha256> SUPPLIER = JavaSha256::new;

    private final MessageDigest digest;

    private JavaSha256() {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // Can't possibly happen as SHA-256 is required by the MessageDigest class to be present.
            throw new AssertionError(e);
        }
    }

    @Override
    public void update(byte[] input, int offset, int len) {
        this.digest.update(input, offset, len);
    }

    @Override
    public void update(ByteBuffer buffer) {
        this.digest.update(buffer);
    }

    @Override
    public byte[] digest() {
        return this.digest.digest();
    }

    @Override
    public void reset() {
        this.digest.reset();
    }

    @Override
    public void free() {
        // no-op
    }
}
