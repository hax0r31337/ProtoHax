package com.nukkitx.natives;


import javax.crypto.ShortBufferException;
import java.nio.ByteBuffer;

public interface NativeCipher extends Native {

    void cipher(ByteBuffer input, ByteBuffer output) throws ShortBufferException, IllegalArgumentException;
}
