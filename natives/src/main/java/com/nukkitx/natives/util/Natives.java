package com.nukkitx.natives.util;

import com.nukkitx.natives.NativeCode;
import com.nukkitx.natives.zlib.Zlib;

public class Natives {

    public static final NativeCode<Zlib> ZLIB = new NativeCode<>(
            Zlib.JAVA
    );
}
