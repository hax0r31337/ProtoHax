package com.nukkitx.network;

import java.net.InetSocketAddress;

public interface NetworkListener {

    boolean bind();

    void close();

    InetSocketAddress getAddress();
}
