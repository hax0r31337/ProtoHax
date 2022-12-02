package com.nukkitx.natives.aes;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

@FunctionalInterface
public interface AesFactory {

    Aes get(boolean encrypt, SecretKey key, IvParameterSpec iv);
}
