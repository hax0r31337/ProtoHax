package com.nukkitx.natives.crc32c;

import java.nio.ByteBuffer;
import java.util.function.Supplier;
import java.util.zip.CRC32C;

public class JavaCrc32C implements Crc32C {
    public static final Supplier<Crc32C> SUPPLIER = JavaCrc32C::new;
    private final CRC32C crc32c = new CRC32C();

    private JavaCrc32C() {
    }

    @Override
    public void update(int b) {
        this.crc32c.update(b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        this.crc32c.update(b, off, len);
    }

    @Override
    public void update(ByteBuffer buffer) {
        this.crc32c.update(buffer);
    }

    @Override
    public long getValue() {
        return this.crc32c.getValue();
    }

    @Override
    public void reset() {
        this.crc32c.reset();
    }

    @Override
    public void free() {
        // no-op
    }
}
