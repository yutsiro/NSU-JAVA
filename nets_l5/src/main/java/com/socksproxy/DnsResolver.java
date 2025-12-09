package com.socksproxy;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DnsResolver {
    private final DatagramChannel channel;
    private final Map<Integer, DnsCallback> pendingQueries = new ConcurrentHashMap<>();
    private int nextId = 1;

    public DnsResolver(Selector selector) throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.bind(null);
        channel.register(selector, SelectionKey.OP_READ);
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    public void resolve(String domain, DnsCallback callback) throws IOException {
        try {
            Message query = Message.newQuery(
                    Record.newRecord(
                            Name.fromString(domain.endsWith(".") ? domain : domain + "."),
                            Type.A,
                            DClass.IN
                    )
            );

            int id = nextId++;
            query.getHeader().setID(id);
            pendingQueries.put(id, callback);

            byte[] data = query.toWire();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            channel.send(buffer, new InetSocketAddress("8.8.8.8", 53));
        } catch (Exception e) {
            System.err.println("DNS query failed for " + domain + ": " + e.getMessage());
            callback.onResolved(null);
        }
    }

    public void handleRead() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        InetSocketAddress sender = (InetSocketAddress) channel.receive(buffer);

        if (sender == null) return;

        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        try {
            Message response = new Message(data);
            int id = response.getHeader().getID();
            DnsCallback callback = pendingQueries.remove(id);

            if (callback != null) {
                String ip = extractIp(response);
                callback.onResolved(ip);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse DNS response: " + e.getMessage());
        }
    }

    private String extractIp(Message response) {
        Record[] answers = response.getSectionArray(Section.ANSWER);
        if (answers != null) {
            for (Record record : answers) {
                if (record instanceof ARecord) {
                    return ((ARecord) record).getAddress().getHostAddress();
                }
            }
        }
        return null;
    }

    public interface DnsCallback {
        void onResolved(String ip);
    }
}