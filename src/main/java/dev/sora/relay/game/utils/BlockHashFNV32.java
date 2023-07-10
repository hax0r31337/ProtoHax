package dev.sora.relay.game.utils;

import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * https://gist.github.com/Alemiz112/504d0f79feac7ef57eda174b668dd345
 *
 * @author Alemiz112
 */
public class BlockHashFNV32 {

    private static final int FNV1_32_INIT = 0x811c9dc5;
    private static final int FNV1_PRIME_32 = 0x01000193;

    public static int createHash(NbtMap block) {
        if (block.getString("name").equals("minecraft:unknown")) {
            return -2; // This is special case
        }

        byte[] bytes;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             NBTOutputStream outputStream = NbtUtils.createWriterLE(stream)) {
            outputStream.writeTag(block);
            bytes = stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fnv1a_32(bytes);
    }

    public static int fnv1a_32(byte[] data) {
        int hash = FNV1_32_INIT;
        for (byte datum : data) {
            hash ^= (datum & 0xff);
            hash *= FNV1_PRIME_32;
        }
        return hash;
    }
}
