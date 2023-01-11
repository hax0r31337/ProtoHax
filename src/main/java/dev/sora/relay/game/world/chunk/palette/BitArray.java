package dev.sora.relay.game.world.chunk.palette;

/**
 * from nukkit https://github.com/CloudburstMC/Nukkit/
 */
public interface BitArray {

    void set(int index, int value);

    int get(int index);

    int size();

    int[] getWords();

    BitArrayVersion getVersion();

    BitArray copy();

    static int ceil(float floatNumber) {
        int truncated = (int) floatNumber;
        return floatNumber > truncated ? truncated + 1 : truncated;
    }
}
