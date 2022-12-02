package com.nukkitx.network.raknet;

import com.nukkitx.network.NetworkUtils;
import com.nukkitx.network.util.DisconnectReason;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;

import javax.annotation.ParametersAreNonnullByDefault;
import java.net.Inet6Address;
import java.net.InetSocketAddress;

import static com.nukkitx.network.raknet.RakNetConstants.*;

@ParametersAreNonnullByDefault
public class RakNetClientSession extends RakNetSession {
    private final RakNetClient rakNet;
    private int connectionAttempts;
    private long nextConnectionAttempt;

    RakNetClientSession(RakNetClient rakNet, InetSocketAddress address, Channel channel, EventLoop eventLoop, int mtu,
                        int protocolVersion) {
        super(address, channel, eventLoop, mtu, protocolVersion);
        this.rakNet = rakNet;
    }

    @Override
    protected void onPacket(ByteBuf buffer) {
        int packetId = buffer.readUnsignedByte();

        switch (packetId) {
            case ID_OPEN_CONNECTION_REPLY_1:
                this.onOpenConnectionReply1(buffer);
                break;
            case ID_OPEN_CONNECTION_REPLY_2:
                this.onOpenConnectionReply2(buffer);
                break;
            case ID_CONNECTION_REQUEST_ACCEPTED:
                this.onConnectionRequestAccepted(buffer);
                break;
            case ID_CONNECTION_REQUEST_FAILED:
                this.close(DisconnectReason.CONNECTION_REQUEST_FAILED);
                break;
            case ID_INCOMPATIBLE_PROTOCOL_VERSION:
                this.close(DisconnectReason.INCOMPATIBLE_PROTOCOL_VERSION);
                break;
            case ID_ALREADY_CONNECTED:
                this.close(DisconnectReason.ALREADY_CONNECTED);
                break;
            case ID_NO_FREE_INCOMING_CONNECTIONS:
                this.close(DisconnectReason.NO_FREE_INCOMING_CONNECTIONS);
                break;
            case ID_IP_RECENTLY_CONNECTED:
                this.close(DisconnectReason.IP_RECENTLY_CONNECTED);
                break;
        }
    }

    @Override
    protected void tick(long curTime) {
        if (this.getState() == RakNetState.UNCONNECTED) {
            if (this.connectionAttempts >= MAXIMUM_CONNECTION_ATTEMPTS) {
                this.close(DisconnectReason.TIMED_OUT);
            } else {
                if (this.nextConnectionAttempt < curTime) {
                    this.attemptConnection(curTime);
                }
            }
        }
        
        super.tick(curTime);
    }

    private void attemptConnection(long curTime) {
        int mtuDiff = (MAXIMUM_MTU_SIZE - MINIMUM_MTU_SIZE) / 9;
        int mtuSize = MAXIMUM_MTU_SIZE - (this.connectionAttempts * mtuDiff);
        if (mtuSize < MINIMUM_MTU_SIZE) {
            mtuSize = MINIMUM_MTU_SIZE;
        }

        this.sendOpenConnectionRequest1(mtuSize);

        this.nextConnectionAttempt = curTime + 1000;
        this.connectionAttempts++;
    }

    @Override
    public RakNet getRakNet() {
        return this.rakNet;
    }

    private void onOpenConnectionReply1(ByteBuf buffer) {
        if (this.getState() != RakNetState.UNCONNECTED) {
            return;
        }
        if (!RakNetUtils.verifyUnconnectedMagic(buffer)) {
            return;
        }
        this.guid = buffer.readLong();
        boolean security = buffer.readBoolean();
        int mtu = buffer.readUnsignedShort();
        this.setMtu(mtu);

        if (security) {
            this.close(DisconnectReason.CONNECTION_REQUEST_FAILED);
            return;
        }

        this.setState(RakNetState.INITIALIZING);

        this.sendOpenConnectionRequest2();
    }

    private void onOpenConnectionReply2(ByteBuf buffer) {
        if (this.getState() != RakNetState.INITIALIZING) {
            return;
        }
        if (!RakNetUtils.verifyUnconnectedMagic(buffer)) {
            this.close(DisconnectReason.CONNECTION_REQUEST_FAILED);
            return;
        }

        long guid = buffer.readLong();
        if (this.guid != guid) {
            this.close(DisconnectReason.CONNECTION_REQUEST_FAILED);
            return;
        }
        InetSocketAddress address = NetworkUtils.readAddress(buffer);
        int mtu = buffer.readUnsignedShort();
        this.setMtu(mtu);
        boolean security = buffer.readBoolean();

        this.initialize();
        this.setState(RakNetState.INITIALIZED);

        this.sendConnectionRequest();
    }

    private void onConnectionRequestAccepted(ByteBuf buffer) {
        NetworkUtils.readAddress(buffer); // our address
        buffer.readUnsignedShort(); // system index
        final int required = IPV4_MESSAGE_SIZE + 16; // Address + 2 * Long - Minimum amount of data
        long pongTime = 0;
        try {
            while (buffer.isReadable(required)) {
                NetworkUtils.readAddress(buffer);
            }
            pongTime = buffer.readLong();
            buffer.readLong();
        } catch (IndexOutOfBoundsException ignored) {
            // Hive sends malformed IPv6 address
        }

        this.sendNewIncomingConnection(pongTime);

        this.setState(RakNetState.CONNECTED);
    }

    private void sendOpenConnectionRequest1(int mtuSize) {
        ByteBuf buffer = this.allocateBuffer(mtuSize);
        buffer.writeByte(ID_OPEN_CONNECTION_REQUEST_1);
        RakNetUtils.writeUnconnectedMagic(buffer);
        buffer.writeByte(this.protocolVersion);
        buffer.writeZero(mtuSize - 1 - 16 - 1 - (this.address.getAddress() instanceof Inet6Address ? 40 : 20)
                - UDP_HEADER_SIZE); // 1 (Packet ID), 16 (Magic), 1 (Protocol Version), 20/40 (IP Header));

        this.sendDirect(buffer);
    }

    private void sendOpenConnectionRequest2() {
        ByteBuf buffer = this.allocateBuffer(34);
        buffer.writeByte(ID_OPEN_CONNECTION_REQUEST_2);
        RakNetUtils.writeUnconnectedMagic(buffer);
        NetworkUtils.writeAddress(buffer, this.address);
        buffer.writeShort(this.getMtu());
        buffer.writeLong(this.rakNet.guid);

        this.sendDirect(buffer);
    }

    private void sendConnectionRequest() {
        ByteBuf buffer = this.allocateBuffer(18);

        buffer.writeByte(ID_CONNECTION_REQUEST);
        buffer.writeLong(this.rakNet.guid);
        buffer.writeLong(System.currentTimeMillis());
        buffer.writeBoolean(false);

        this.send(buffer, RakNetPriority.IMMEDIATE, RakNetReliability.RELIABLE_ORDERED);
    }

    private void sendNewIncomingConnection(long pingTime) {
        boolean ipv6 = this.isIpv6Session();
        ByteBuf buffer = this.allocateBuffer(ipv6 ? 626 : 164);

        buffer.writeByte(ID_NEW_INCOMING_CONNECTION);
        NetworkUtils.writeAddress(buffer, address);
        for (InetSocketAddress address : ipv6 ? LOCAL_IP_ADDRESSES_V6 : LOCAL_IP_ADDRESSES_V4) {
            NetworkUtils.writeAddress(buffer, address);
        }
        buffer.writeLong(pingTime);
        buffer.writeLong(System.currentTimeMillis());

        this.send(buffer, RakNetPriority.IMMEDIATE, RakNetReliability.RELIABLE_ORDERED);
    }
}
