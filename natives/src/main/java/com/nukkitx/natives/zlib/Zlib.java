package com.nukkitx.natives.zlib;

import java.util.function.Supplier;

public final class Zlib {
    static final int CHUNK_BYTES = 8192;

    private static final Zlib ZLIB_JAVA = new Zlib(JavaInflater::new, JavaDeflater::new);
    public static final Supplier<Zlib> JAVA = () -> ZLIB_JAVA;

    private final InflaterFactory inflaterFactory;
    private final DeflaterFactory deflaterFactory;

    private Zlib(InflaterFactory inflaterFactory, DeflaterFactory deflaterFactory) {
        this.inflaterFactory = inflaterFactory;
        this.deflaterFactory = deflaterFactory;
    }

    public Inflater create(boolean nowrap) {
        return this.inflaterFactory.create(nowrap);
    }

    public Deflater create(int level, boolean nowrap) {
        return this.deflaterFactory.create(level, nowrap);
    }

    @FunctionalInterface
    interface InflaterFactory {

        Inflater create(boolean nowrap);
    }

    @FunctionalInterface
    interface DeflaterFactory {

        Deflater create(int level, boolean nowrap);
    }
}
