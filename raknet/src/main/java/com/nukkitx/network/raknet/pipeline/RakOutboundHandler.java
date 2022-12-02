package com.nukkitx.network.raknet.pipeline;

import com.nukkitx.network.raknet.RakNet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;

public class RakOutboundHandler extends ChannelOutboundHandlerAdapter {
    public static final String NAME = "rak-outbound-handler";
    private final RakNet rakNet;

    public RakOutboundHandler(RakNet rakNet) {
        this.rakNet = rakNet;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof DatagramPacket)) {
            super.write(ctx, msg, promise);
            return;
        }

        ByteBuf buffer = ((DatagramPacket) msg).content();
        if (this.rakNet.getMetrics() != null) {
            this.rakNet.getMetrics().bytesOut(buffer.readableBytes());
        }
        super.write(ctx, msg, promise);
    }
}
