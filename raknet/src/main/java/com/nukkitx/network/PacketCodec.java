package com.nukkitx.network;

import io.netty.buffer.ByteBuf;

public interface PacketCodec<T extends NetworkPacket> {

    T tryDecode(ByteBuf byteBuf);

    ByteBuf tryEncode(T packet);

    int getId(T packet);
}
