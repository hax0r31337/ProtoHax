package com.nukkitx.network.util;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadFactory;

@UtilityClass
public final class EventLoops {
    private static EventLoopGroup EVENT_LOOP_GROUP;
    private static final ThreadFactory EVENT_LOOP_FACTORY = NetworkThreadFactory.builder().format("Network Listener - #%d")
            .daemon(true).build();

    @Getter
    private static final Class<? extends DatagramChannel> datagramChannel = NioDatagramChannel.class;
    @Getter
    private static final Class<? extends SocketChannel> socketChannel =  NioSocketChannel.class;
    @Getter
    private static final Class<? extends ServerSocketChannel> serverSocketChannel = NioServerSocketChannel.class;

    public static EventLoopGroup commonGroup() {
        if (EVENT_LOOP_GROUP == null) {
            EVENT_LOOP_GROUP = newEventLoopGroup(0);
        }
        return EVENT_LOOP_GROUP;
    }

    public static EventLoopGroup newEventLoopGroup(int threads) {
        return new NioEventLoopGroup(threads, EVENT_LOOP_FACTORY);
    }
}
