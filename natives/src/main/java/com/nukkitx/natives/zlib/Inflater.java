package com.nukkitx.natives.zlib;

import com.nukkitx.natives.Native;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

public interface Inflater extends Native {

    void setInput(ByteBuffer input);

    int inflate(ByteBuffer output) throws DataFormatException;

    int getAdler();

    boolean finished();

    void reset();

    long getBytesRead();

    interface Factory {
        Inflater newInstance(boolean nowrap);
    }
}
