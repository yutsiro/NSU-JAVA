package com.socksproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ClientSession implements DnsResolver.DnsCallback {
    private enum State { HANDSHAKE, REQUEST, FORWARDING, CLOSED }

    private final SocketChannel client;
    private final SelectionKey clientKey;
    private final Selector selector;
    private final DnsResolver dns;

    private SocketChannel remote;
    private SelectionKey remoteKey;
    private State state = State.HANDSHAKE;

    private final ByteBuffer clientBuffer = ByteBuffer.allocate(8192);
    private final ByteBuffer remoteBuffer = ByteBuffer.allocate(8192);

    private String pendingAddress;
    private int pendingPort;

    public ClientSession(SocketChannel client, SelectionKey key, DnsResolver dns, Selector selector) {
        this.client = client;
        this.clientKey = key;
        this.selector = selector;
        this.dns = dns;
        key.attach(this);
    }

    public void handle(SelectionKey key) throws IOException {
        if (state == State.CLOSED) return;

        try {
            if (key.isReadable()) {
                if (key.channel() == client) {
                    handleClientRead();
                } else if (key.channel() == remote) {
                    handleRemoteRead();
                }
            } else if (key.isConnectable() && key.channel() == remote) {
                finishConnect();
            }
        } catch (IOException e) {
            close();
        }
    }

    private void handleClientRead() throws IOException {
        int read = client.read(clientBuffer);
        if (read == -1) {
            close();
            return;
        }

        if (read > 0) {
            clientBuffer.flip();

            switch (state) {
                case HANDSHAKE:
                    handleHandshake();
                    break;
                case REQUEST:
                    handleRequest();
                    break;
                case FORWARDING:
                    forwardToRemote(clientBuffer);
                    break;
            }

            clientBuffer.compact();
        }
    }

    private void handleHandshake() throws IOException {
        if (clientBuffer.remaining() < 2) return;

        int start = clientBuffer.position();
        byte ver = clientBuffer.get();
        int nmethods = clientBuffer.get() & 0xFF;

        if (clientBuffer.remaining() < nmethods) {
            clientBuffer.position(start);
            return;
        }

        clientBuffer.position(clientBuffer.position() + nmethods);

        if (ver == 5) {
            ByteBuffer response = ByteBuffer.allocate(2);
            response.put((byte) 5);
            response.put((byte) 0); //без аутентификации
            response.flip();
            client.write(response);
            state = State.REQUEST;
        } else {
            close();
        }
    }

    private void handleRequest() throws IOException {
        SocksRequest request = SocksRequest.parse(clientBuffer);
        if (request == null) return;

        if (request.isDomain()) {
            pendingAddress = request.domain;
            pendingPort = request.port;
            dns.resolve(request.domain, this);
        } else {
            connectDirect(request.address, request.port);
        }
    }

    private void handleRemoteRead() throws IOException {
        int read = remote.read(remoteBuffer);
        if (read == -1) {
            close();
            return;
        }

        if (read > 0) {
            remoteBuffer.flip();
            client.write(remoteBuffer);
            remoteBuffer.compact();
        }
    }

    private void forwardToRemote(ByteBuffer buffer) throws IOException {
        if (remote != null && remote.isConnected()) {
            remote.write(buffer);
        }
    }

    @Override
    public void onResolved(String ip) {
        try {
            if (ip == null) {
                SocksRequest.sendFailure(client);
                close();
            } else {
                connectDirect(ip, pendingPort);
            }
        } catch (IOException e) {
            close();
        }
    }

    private void connectDirect(String address, int port) throws IOException {
        remote = SocketChannel.open();
        remote.configureBlocking(false);
        remoteKey = remote.register(selector, SelectionKey.OP_CONNECT);
        remoteKey.attach(this);
        remote.connect(new InetSocketAddress(address, port));
    }

    private void finishConnect() throws IOException {
        if (remote.finishConnect()) {
            SocksRequest.sendSuccess(client);
            state = State.FORWARDING;

            remoteKey.interestOps(SelectionKey.OP_READ);
            clientKey.interestOps(SelectionKey.OP_READ);
        }
    }

    private void close() {
        if (state == State.CLOSED) return;

        state = State.CLOSED;

        if (clientKey != null) {
            clientKey.cancel();
        }

        if (remoteKey != null) {
            remoteKey.cancel();
        }

        try {
            if (client != null && client.isOpen()) {
                client.close();
            }
        } catch (IOException ignored) {}

        try {
            if (remote != null && remote.isOpen()) {
                remote.close();
            }
        } catch (IOException ignored) {}

        System.out.println("Client session closed");
    }
}