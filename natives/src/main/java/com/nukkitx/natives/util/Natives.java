package com.nukkitx.natives.util;

import com.nukkitx.natives.NativeCode;
import com.nukkitx.natives.aes.AesFactory;
import com.nukkitx.natives.aes.JavaAes;
import com.nukkitx.natives.crc32c.Crc32C;
import com.nukkitx.natives.crc32c.JavaCrc32C;
import com.nukkitx.natives.sha256.JavaSha256;
import com.nukkitx.natives.sha256.Sha256;
import com.nukkitx.natives.zlib.Zlib;

import java.util.function.BooleanSupplier;

public class Natives {

    public static final NativeCode<Crc32C> CRC32C = new NativeCode<>(
            JavaCrc32C.SUPPLIER
    );

    public static final NativeCode<Sha256> SHA_256 = new NativeCode<>(
            JavaSha256.SUPPLIER
    );

    public static final NativeCode<AesFactory> AES_CFB8 = new NativeCode<>(
            JavaAes.SUPPLIER
    );

    public static final NativeCode<Zlib> ZLIB = new NativeCode<>(
            Zlib.JAVA
    );
}
