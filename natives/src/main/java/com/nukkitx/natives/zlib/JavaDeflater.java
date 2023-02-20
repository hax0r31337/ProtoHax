package com.nukkitx.natives.zlib;

import java.nio.ByteBuffer;

public class JavaDeflater implements Deflater {
    private final java.util.zip.Deflater deflater;

    JavaDeflater(int level, boolean nowrap) {
        this.deflater = new java.util.zip.Deflater(level, nowrap);
    }

    @Override
    public void setLevel(int level) {
        this.deflater.setLevel(level);
    }

    @Override
    public void setInput(ByteBuffer input) {
        this.deflater.setInput(input);
    }

    @Override
    public int deflate(ByteBuffer output) {
        this.deflater.finish();
        return this.deflater.deflate(output);
    }

    @Override
    public int getAdler() {
        return this.deflater.getAdler();
    }

    @Override
    public void reset() {
        this.deflater.reset();
    }

    @Override
    public boolean finished() {
        return this.deflater.finished();
    }

    @Override
    public void free() {
        this.deflater.end();
    }
}
