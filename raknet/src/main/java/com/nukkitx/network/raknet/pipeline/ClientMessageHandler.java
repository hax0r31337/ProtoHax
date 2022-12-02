package com.nukkitx.network.raknet.pipeline;

import com.nukkitx.network.raknet.RakNetClient;
import com.nukkitx.network.raknet.RakNetClientSession;
import com.nukkitx.network.raknet.RakNetUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import static com.nukkitx.network.raknet.RakNetConstants.ID_UNCONNECTED_PONG;

public class ClientMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    public static final String NAME = "rak-client-message-handler";

    private final RakNetClient client;

    public ClientMessageHandler(RakNetClient client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        ByteBuf buffer = packet.content();
        if (!buffer.isReadable()) {
            return;
        }

        if (this.client.getMetrics() != null) {
            this.client.getMetrics().bytesIn(buffer.readableBytes());
        }

        int packetId = buffer.readUnsignedByte();
        if (packetId == ID_UNCONNECTED_PONG) {
            this.onUnconnectedPong(packet);
            return;
        }

        final RakNetClientSession session = this.client.getSession();
        if (session == null || !session.getAddress().equals(packet.sender())) {
            return;
        }

        ByteBuf buf = buffer.readerIndex(0).retain();
        if (session.getEventLoop().inEventLoop()) {
            session.onDatagram(buf);
        } else {
            session.getEventLoop().execute(() -> session.onDatagram(buf));
        }
    }

    private void onUnconnectedPong(DatagramPacket packet) {
        ByteBuf content = packet.content();
        long pingTime = content.readLong();
        long guid = content.readLong();
        if (!RakNetUtils.verifyUnconnectedMagic(content)) {
            return;
        }

        byte[] userData = null;
        if (content.isReadable()) {
            userData = new byte[content.readUnsignedShort()];
            content.readBytes(userData);
        }
        this.client.onUnconnectedPong(new RakNetClient.PongEntry(packet.sender(), pingTime, guid, userData));
    }
}
