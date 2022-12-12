package com.nukkitx.network.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.ServerSocketChannel;
import lombok.experimental.UtilityClass;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@UtilityClass
public final class Bootstraps {

    public static void setupBootstrap(Bootstrap bootstrap, boolean datagram) {
        Class<? extends Channel> channel = datagram ? EventLoops.getDatagramChannel() : EventLoops.getSocketChannel();
        bootstrap.channel(channel);
    }

//    public static void setupServerBootstrap(ServerBootstrap bootstrap) {
//        Class<? extends ServerSocketChannel> channel = EventLoops.getServerSocketChannel();
//        bootstrap.channel(channel);
//    }
//
//    private static int[] fromString(String ver) {
//        String[] parts = ver.split("\\.");
//        if (parts.length < 2) {
//            throw new IllegalArgumentException("At least 2 version numbers required");
//        }
//
//        return new int[]{
//                Integer.parseInt(parts[0]),
//                Integer.parseInt(parts[1]),
//                parts.length == 2 ? 0 : Integer.parseInt(parts[2])
//        };
//    }

    public static CompletableFuture<Void> allOf(ChannelFuture... futures) {
        if (futures == null || futures.length == 0) {
            return CompletableFuture.completedFuture(null);
        }
        @SuppressWarnings("unchecked")
        CompletableFuture<Channel>[] completableFutures = new CompletableFuture[futures.length];
        for (int i = 0; i < futures.length; i++) {
            ChannelFuture channelFuture = futures[i];
            CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
            channelFuture.addListener(future -> {
                if (future.cause() != null) {
                    completableFuture.completeExceptionally(future.cause());
                }
                completableFuture.complete(channelFuture.channel());
            });
            completableFutures[i] = completableFuture;
        }

        return CompletableFuture.allOf(completableFutures);
    }
}
