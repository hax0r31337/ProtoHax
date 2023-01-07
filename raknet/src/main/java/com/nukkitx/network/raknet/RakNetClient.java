package com.nukkitx.network.raknet;

import com.nukkitx.network.raknet.pipeline.ClientMessageHandler;
import com.nukkitx.network.raknet.pipeline.RakExceptionHandler;
import com.nukkitx.network.raknet.pipeline.RakOutboundHandler;
import com.nukkitx.network.util.EventLoops;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.nukkitx.network.raknet.RakNetConstants.*;

@ParametersAreNonnullByDefault
public class RakNetClient extends RakNet {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(RakNetClient.class);
    private final Map<InetSocketAddress, PingEntry> pings = new ConcurrentHashMap<>();

    protected InetSocketAddress bindAddress;
    protected RakNetClientSession session;
    private Channel channel;
    private EventLoop tickingEventLoop;
    private final NettyChannelInitializer handler;

    public RakNetClient() {
        this(null, EventLoops.commonGroup(), null);
    }

    public RakNetClient(InetSocketAddress bindAddress) {
        this(bindAddress, EventLoops.commonGroup(), null);
    }

    public RakNetClient(InetSocketAddress bindAddress, EventLoopGroup eventLoopGroup, NettyChannelInitializer handler) {
        super(eventLoopGroup);
        this.handler = handler;
        this.bindAddress = bindAddress;
        this.exceptionHandlers.put("DEFAULT", (t) -> log.error("An exception occurred in RakNet Client, address="+bindAddress, t));
    }

    public RakNetClient(@Nullable InetSocketAddress bindAddress, Bootstrap bootstrap, NettyChannelInitializer handler) {
        super(bootstrap);
        this.handler = handler;
        this.bindAddress = bindAddress;
        this.exceptionHandlers.put("DEFAULT", (t) -> log.error("An exception occurred in RakNet Client, address="+bindAddress, t));
    }

    @Override
    protected CompletableFuture<Void> bindInternal() {
        this.bootstrap.handler(new ClientChannelInitializer());
        ChannelFuture channelFuture = this.bindAddress == null? this.bootstrap.bind() : this.bootstrap.bind(this.bindAddress);

        CompletableFuture<Void> future = new CompletableFuture<>();
        channelFuture.addListener((ChannelFuture promise) -> {
            if (promise.cause() != null) {
                future.completeExceptionally(promise.cause());
                return;
            }

            SocketAddress address = promise.channel().localAddress();
            if (!(address instanceof InetSocketAddress)) {
                future.completeExceptionally(new IllegalArgumentException("Excepted InetSocketAddress but got "+address.getClass().getSimpleName()));
                return;
            }
            this.bindAddress = (InetSocketAddress) address;
            future.complete(null);
        });
        return future;
    }

    public RakNetClientSession connect(InetSocketAddress address) {
        if (!this.isRunning()) {
            throw new IllegalStateException("RakNet has not been started");
        }
        if (this.session != null) {
            throw new IllegalStateException("Session has already been created");
        }

        this.session = new RakNetClientSession(this, address, this.channel, this.channel.eventLoop(),
                MAXIMUM_MTU_SIZE, this.protocolVersion);
        return this.session;
    }

    public CompletableFuture<RakNetPong> ping(InetSocketAddress address, long timeout, TimeUnit unit) {
        if (!this.isRunning()) {
            throw new IllegalStateException("RakNet has not been started");
        }

        if (this.session != null && this.session.address.equals(address)) {
            throw new IllegalArgumentException("Cannot ping connected address");
        }
        if (this.pings.containsKey(address)) {
            return this.pings.get(address).future;
        }

        long curTime = System.currentTimeMillis();
        CompletableFuture<RakNetPong> pongFuture = new CompletableFuture<>();

        PingEntry entry = new PingEntry(pongFuture, curTime + unit.toMillis(timeout));
        entry.sendTime = curTime;
        this.pings.put(address, entry);
        this.sendUnconnectedPing(address);
        return pongFuture;
    }

    @Override
    protected void onTick() {
        final long curTime = System.currentTimeMillis();
        final RakNetClientSession session = this.session;
        if (session != null && !session.isClosed()) {
            session.eventLoop.execute(() -> session.onTick(curTime));
        }

        Iterator<Map.Entry<InetSocketAddress, PingEntry>> iterator = this.pings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<InetSocketAddress, PingEntry> entry = iterator.next();
            PingEntry ping = entry.getValue();
            if (curTime >= ping.timeout) {
                ping.future.completeExceptionally(new TimeoutException());
                iterator.remove();
            } else if ((curTime - ping.sendTime) >= RAKNET_PING_INTERVAL) {
                ping.sendTime = curTime;
                this.sendUnconnectedPing(entry.getKey());
            }
        }
    }

    public void onUnconnectedPong(PongEntry entry) {
        EventLoop eventLoop = this.nextEventLoop();
        if (eventLoop.inEventLoop()) {
            this.onUnconnectedPong0(entry);
        } else {
            eventLoop.execute(() -> this.onUnconnectedPong0(entry));
        }
    }

    private void onUnconnectedPong0(PongEntry pong) {
        PingEntry ping = this.pings.remove(pong.address);
        if (ping != null) {
            ping.future.complete(new RakNetPong(pong.pingTime, System.currentTimeMillis(), pong.guid, pong.userData));
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Received unexcepted pong from " + pong.address);
        }
    }

    @Override
    public void close(boolean force) {
        super.close(force);
        if (this.session != null && !this.session.isClosed()) {
            this.session.close();
        }

        if (this.channel != null) {
            ChannelFuture future = this.channel.close();
            if (force) future.syncUninterruptibly();
        }
    }

    private void sendUnconnectedPing(InetSocketAddress recipient) {
        ByteBuf buffer = this.channel.alloc().ioBuffer(23);
        buffer.writeByte(ID_UNCONNECTED_PING);
        buffer.writeLong(System.currentTimeMillis());
        RakNetUtils.writeUnconnectedMagic(buffer);
        buffer.writeLong(this.guid);

        this.channel.writeAndFlush(new DatagramPacket(buffer, recipient));
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return this.bindAddress;
    }

    public RakNetClientSession getSession() {
        return this.session;
    }

    @Override
    protected EventLoop nextEventLoop() {
        if (this.tickingEventLoop == null) {
            this.tickingEventLoop = super.nextEventLoop();
        }
        return this.tickingEventLoop;
    }

    @RequiredArgsConstructor
    public static class PingEntry {
        private final CompletableFuture<RakNetPong> future;
        private final long timeout;
        private long sendTime;
    }

    @RequiredArgsConstructor
    public static class PongEntry {
        private final InetSocketAddress address;
        private final long pingTime;
        private final long guid;
        private final byte[] userData;
    }

    private class ClientChannelInitializer extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel channel) throws Exception {
            if (handler != null) {
                handler.initChannel(channel);
            }
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(RakOutboundHandler.NAME, new RakOutboundHandler(RakNetClient.this));
            pipeline.addLast(ClientMessageHandler.NAME, new ClientMessageHandler(RakNetClient.this));
            pipeline.addLast(RakExceptionHandler.NAME, new RakExceptionHandler(RakNetClient.this));
            RakNetClient.this.channel = channel;
        }
    }

    public interface NettyChannelInitializer {
        public void initChannel(Channel channel);
    }
}
