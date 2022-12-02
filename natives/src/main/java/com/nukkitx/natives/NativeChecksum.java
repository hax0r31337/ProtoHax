package com.nukkitx.natives;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;

public interface NativeChecksum extends Checksum, Native {

    void update(ByteBuffer buffer);
}
