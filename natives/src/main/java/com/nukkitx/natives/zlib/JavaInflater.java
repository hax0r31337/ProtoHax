package com.nukkitx.natives.zlib;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

public class JavaInflater implements Inflater {

    private final java.util.zip.Inflater inflater;

    JavaInflater(boolean nowrap) {
        this.inflater = new java.util.zip.Inflater(nowrap);
    }

    @Override
    public void setInput(ByteBuffer input) {
        this.inflater.setInput(input);
    }

    @Override
    public int inflate(ByteBuffer output) throws DataFormatException {
        return this.inflater.inflate(output);
    }

    @Override
    public int getAdler() {
        return this.inflater.getAdler();
    }

    @Override
    public boolean finished() {
        return this.inflater.finished();
    }

    @Override
    public void reset() {
        this.inflater.reset();
    }

    @Override
    public long getBytesRead() {
        return this.inflater.getBytesRead();
    }

    @Override
    public void free() {
        this.inflater.end();
    }
}
