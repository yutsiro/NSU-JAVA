package val.network;


import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class GameAnnounce implements Runnable{

    @Setter
    private SnakesProto.GameMessage announcement;
    private DatagramSocket out;

    public GameAnnounce(SnakesProto.GameMessage announcement, DatagramSocket out) {
        this.announcement = announcement;
        this.out = out;
    }

    @Override
    public void run() {
        InetAddress address;
        try {
            address = InetAddress.getByName("239.192.0.4");
            int port = 9192;

            byte[] data = announcement.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, port);

            while (!Thread.currentThread().isInterrupted()) {
                out.send(packet);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }
    }
}
