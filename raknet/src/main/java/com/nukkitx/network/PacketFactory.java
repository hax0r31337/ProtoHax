package com.nukkitx.network;

@FunctionalInterface
@SuppressWarnings("unchecked")
public interface PacketFactory<T extends NetworkPacket> {
    T newInstance();

    default Class<T> getPacketClass() {
        return (Class<T>) newInstance().getClass();
    }
}
