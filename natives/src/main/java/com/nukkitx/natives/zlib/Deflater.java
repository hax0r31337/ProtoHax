package com.nukkitx.natives.zlib;

import com.nukkitx.natives.Native;

import java.nio.ByteBuffer;

public interface Deflater extends Native {

    void setLevel(int level);

    void setInput(ByteBuffer input);

    int deflate(ByteBuffer output);

    int getAdler();

    void reset();

    boolean finished();

    interface Factory {

        Deflater newInstance(int level, boolean nowrap);
    }
}
