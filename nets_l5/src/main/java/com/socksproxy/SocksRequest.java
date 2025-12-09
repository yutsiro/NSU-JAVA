package com.socksproxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocksRequest {
    public String domain;
    public String address;
    public int port;

    public boolean isDomain() {
        return domain != null;
    }

    public static SocksRequest parse(ByteBuffer buf) {
        if (buf.remaining() < 7) return null;

        byte ver = buf.get();
        byte cmd = buf.get();
        buf.get();
        byte atyp = buf.get();

        if (ver != 5 || cmd != 1) return null;

        SocksRequest r = new SocksRequest();

        if (atyp == 1) {
            if (buf.remaining() < 6) return null;

            byte[] a = new byte[4];
            buf.get(a);
            r.address = (a[0] & 0xFF) + "." + (a[1] & 0xFF) + "." +
                    (a[2] & 0xFF) + "." + (a[3] & 0xFF);
        } else if (atyp == 3) {
            int len = buf.get() & 0xFF;
            if (buf.remaining() < len + 2) return null;

            byte[] a = new byte[len];
            buf.get(a);
            r.domain = new String(a);
        } else {
            return null;
        }

        r.port = ((buf.get() & 0xFF) << 8) | (buf.get() & 0xFF);
        return r;
    }

    public static void sendFailure(SocketChannel c) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(new byte[]{
                5, 1, 0, 1,
                0, 0, 0, 0,
                0, 0
        });
        c.write(b);
    }

    public static void sendSuccess(SocketChannel c) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(new byte[]{
                5, 0, 0, 1,
                0, 0, 0, 0,
                0, 0
        });
        c.write(b);
    }
}