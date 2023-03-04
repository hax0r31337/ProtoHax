package com.nukkitx.natives.zlib;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

public class JavaInflater implements Inflater {

    private final byte[] chunkBytes = new byte[Zlib.CHUNK_BYTES];
    private final java.util.zip.Inflater inflater;

    JavaInflater(boolean nowrap) {
        this.inflater = new java.util.zip.Inflater(nowrap);
    }

    @Override
    public void setInput(ByteBuffer input) {
        if (input.hasArray()) {
            this.inflater.setInput(input.array(), input.arrayOffset() + input.position(), input.remaining());
        } else {
            byte[] bytes = new byte[input.remaining()];
            input.get(bytes);
            this.inflater.setInput(bytes);
        }
    }

    @Override
    public int inflate(ByteBuffer output) throws DataFormatException {
        if (output.hasArray()) {
            return this.inflater.inflate(output.array(), output.arrayOffset() + output.position(), output.remaining());
        } else {
            int startPos = output.position();
            while (output.remaining() > 0 && !this.inflater.finished()) {
                int length = Math.min(output.remaining(), Zlib.CHUNK_BYTES);
                int result = this.inflater.inflate(this.chunkBytes, 0, length);
                output.put(this.chunkBytes, 0, result);
            }
            return output.position() - startPos;
        }
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
