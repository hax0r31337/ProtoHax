package dev.sora.relay.game.world.leveldb;

import dev.sora.relay.game.utils.constants.Dimension;

/**
 * From PowerNukkitX
 * @author Superrice666
 */
public enum LevelDBChunkKey {
    /**
     * The version of the chunk
     */
    VERSION(0x2c), // 44

    /**
     * The version of the chunk used before Minecraft 1.2
     */
    @Deprecated
    OLD_VERSION(0x76), // 118

    /**
     * 2D HeightMap and BiomeMap used before Minecraft 1.18
     */
    @Deprecated
    DATA_2D(0x2d), // 45

    /**
     * 3D HeightMap and BiomeMap
     */
    DATA_3D(0x2b), // 43

    /**
     * ChunkSection, contains 16*16*16 data
     * https://wiki.vg/Bedrock_Edition_level_format#SubChunk_serialization
     */
    SUB_CHUNK_DATA(0x2f), // 47

    /**
     * Block Entities in chunks, read until EOF
     */
    BLOCK_ENTITIES(0x31), // 49

    /**
     * Entities data in chunks, read until EOF
     */
    ENTITIES(0x32), // 50

    /**
     * Finalization state for each chunk
     */
    FINALIZATION(0x36); // 54


    private final int id;


    LevelDBChunkKey(int id) {
        this.id = id;
    }

    public byte[] getLevelDBKey(int x, int z) {
        return new byte[]{
                (byte) (x & 0xff), (byte) ((x >> 8) & 0xff), (byte) ((x >> 16) & 0xff), (byte) ((x >> 24) & 0xff),
                (byte) (z & 0xff), (byte) ((z >> 8) & 0xff), (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                (byte) this.id
        };
    }

    public byte[] getLevelDBKey(int x, int z, int extra) {
        return new byte[]{
                (byte) (x & 0xff), (byte) ((x >> 8) & 0xff), (byte) ((x >> 16) & 0xff), (byte) ((x >> 24) & 0xff),
                (byte) (z & 0xff), (byte) ((z >> 8) & 0xff), (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                (byte) this.id, (byte) extra
        };
    }

    public byte[] getLevelDBKeyWithDimension(int x, int z, int dimension) {
        return new byte[]{
                (byte) (x & 0xff), (byte) ((x >> 8) & 0xff), (byte) ((x >> 16) & 0xff), (byte) ((x >> 24) & 0xff),
                (byte) (z & 0xff), (byte) ((z >> 8) & 0xff), (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                (byte) (dimension & 0xff), (byte) ((dimension >> 8) & 0xff), (byte) ((dimension >> 16) & 0xff), (byte) ((dimension >> 24) & 0xff),
                (byte) this.id
        };
    }

    public byte[] getLevelDBKeyWithDimension(int x, int z, int dimension, int extra) {
        return new byte[]{
                (byte) (x & 0xff), (byte) ((x >> 8) & 0xff), (byte) ((x >> 16) & 0xff), (byte) ((x >> 24) & 0xff),
                (byte) (z & 0xff), (byte) ((z >> 8) & 0xff), (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                (byte) (dimension & 0xff), (byte) ((dimension >> 8) & 0xff), (byte) ((dimension >> 16) & 0xff), (byte) ((dimension >> 24) & 0xff),
                (byte) this.id, (byte) extra
        };
    }

    public byte[] getKey(int x, int z, int dimension) {
        if (dimension == Dimension.OVERWORLD) {
            return getLevelDBKey(x, z);
        } else {
            return getLevelDBKeyWithDimension(x, z, dimension);
        }
    }

    public byte[] getKey(int x, int z, int dimension, int extra) {
        if (dimension == Dimension.OVERWORLD) {
            return getLevelDBKey(x, z, extra);
        } else {
            return getLevelDBKeyWithDimension(x, z, dimension, extra);
        }
    }
}
