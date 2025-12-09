package com.socksproxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

public class ProxyServer {
    private final Selector selector; //монитор событий
    private final ServerSocketChannel serverChannel; // сокет для принятия подключений
    private final DnsResolver dns;

    public ProxyServer(int port) throws Exception {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        dns = new DnsResolver(selector); // selector следит и за dns сокетом тоже

        System.out.println("SOCKS5 proxy server started on port " + port);
    }

    public void run() throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            selector.select(); // ждем любого события

            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                try {
                    if (key.isAcceptable()) {
                        acceptClient(); // новый клиент подключился
                    } else if (key.channel() == dns.getChannel()) {
                        dns.handleRead(); // пришел dns - ответ
                    } else {
                        Object attachment = key.attachment();
                        if (attachment instanceof ClientSession) {
                            ((ClientSession) attachment).handle(key); // работа с клиентом
                        }
                    }
                } catch (Exception e) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    private void acceptClient() throws IOException {
        SocketChannel client = serverChannel.accept();
        if (client != null) {
            client.configureBlocking(false);
            SelectionKey key = client.register(selector, SelectionKey.OP_READ);
            new ClientSession(client, key, dns, selector);
            System.out.println("New client connected: " + client.getRemoteAddress());
        }
    }
}