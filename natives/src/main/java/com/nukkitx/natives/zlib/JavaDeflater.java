package com.nukkitx.natives.zlib;

import java.nio.ByteBuffer;

public class JavaDeflater implements Deflater {

    private final byte[] chunkBytes = new byte[Zlib.CHUNK_BYTES];
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
        if (input.hasArray()) {
            this.deflater.setInput(input.array(), input.arrayOffset() + input.position(), input.remaining());
        } else {
            byte[] bytes = new byte[input.remaining()];
            input.get(bytes);
            this.deflater.setInput(bytes);
        }
    }

    @Override
    public int deflate(ByteBuffer output) {
        this.deflater.finish();
        if (output.hasArray()) {
            return this.deflater.deflate(output.array(), output.arrayOffset() + output.position(), output.remaining());
        } else {
            int startPos = output.position();
            while (output.remaining() > 0 && !this.deflater.finished()) {
                int length = Math.min(output.remaining(), Zlib.CHUNK_BYTES);
                int result = this.deflater.deflate(this.chunkBytes, 0, length);
                output.put(this.chunkBytes, 0, result);
            }
            return output.position() - startPos;
        }
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
