package com.nukkitx.natives;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class NativeCode<T> implements Supplier<T> {

    private final Supplier<T> factory;

    public NativeCode(Supplier<T> factory) {
        this.factory = factory;
    }

    @Override
    public T get() {
        return factory.get();
    }
}
