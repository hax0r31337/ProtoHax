package com.nukkitx.network.raknet;

public interface RakMetrics {

    default void bytesIn(int count) {
    }

    default void bytesOut(int count) {
    }

    default void rakDatagramsIn(int count) {
    }

    default void rakDatagramsOut(int count) {
    }

    default void rakStaleDatagrams(int count) {
    }

    default void ackIn(int count) {
    }

    default void ackOut(int count) {
    }

    default void nackIn(int count) {
    }

    default void nackOut(int count) {
    }
}
