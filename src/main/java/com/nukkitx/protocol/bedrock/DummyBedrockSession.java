package com.nukkitx.protocol.bedrock;

import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.compat.BedrockCompat;
import com.nukkitx.protocol.bedrock.data.PacketCompressionAlgorithm;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.zip.Deflater;

public class DummyBedrockSession extends BedrockSession {

    public DummyBedrockSession() {
        this(null);
    }

    public DummyBedrockSession(EventLoop eventLoop) {
        super(null, eventLoop, null);
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void setPacketHandler(@NotNull BedrockPacketHandler packetHandler) {

    }

    @Override
    public void setPacketCodec(BedrockPacketCodec packetCodec) {

    }

    @Override
    void checkForClosed() {

    }

    @Override
    public void sendPacket(@NotNull BedrockPacket packet) {

    }

    @Override
    public void sendPacketImmediately(@NotNull BedrockPacket packet) {

    }

    @Override
    public void sendWrapped(ByteBuf compressed, boolean encrypt) {

    }

    @Override
    public void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt) {

    }

    @Override
    public synchronized void sendWrapped(ByteBuf compressed, boolean encrypt, boolean immediate) {

    }

    @Override
    public void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt, boolean immediate) {

    }

    @Override
    public void sendWrapped(Collection<BedrockPacket> packets, boolean encrypt, boolean immediate, boolean incompressible) {

    }

    @Override
    public void tick() {

    }

    @Override
    public synchronized void enableEncryption(@NotNull SecretKey secretKey) {

    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    void close(DisconnectReason reason) {

    }

    @Override
    public void onWrappedPacket(ByteBuf batched) {

    }

    @Override
    public InetSocketAddress getAddress() {
        return new InetSocketAddress("127.0.0.1", 19132);
    }

    @Override
    public InetSocketAddress getRealAddress() {
        return new InetSocketAddress("127.0.0.1", 19132);
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public BedrockPacketCodec getPacketCodec() {
        return BedrockCompat.COMPAT_CODEC;
    }

    @Override
    public BedrockPacketHandler getPacketHandler() {
        return null;
    }

    @Override
    public BatchHandler getBatchHandler() {
        return null;
    }

    @Override
    public void setBatchHandler(BatchHandler batchHandler) {

    }

    @Override
    public void setCompressionLevel(int compressionLevel) {

    }

    @Override
    public int getCompressionLevel() {
        return Deflater.DEFAULT_COMPRESSION;
    }

    @Override
    public boolean isLogging() {
        return false;
    }

    @Override
    public void setLogging(boolean logging) {

    }

    @Override
    public void addDisconnectHandler(Consumer<DisconnectReason> disconnectHandler) {

    }

    @Override
    public long getLatency() {
        return 8964L;
    }

//    @Override
//    public EventLoop getEventLoop() {
//        return null;
//    }
//
//    @Override
//    public SessionConnection<ByteBuf> getConnection() {
//        return null;
//    }


    @Override
    public void setCompression(PacketCompressionAlgorithm algorithm) {

    }

//    @Override
//    public AtomicInteger getHardcodedBlockingId() {
//        return super.getHardcodedBlockingId();
//    }
}
