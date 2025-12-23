package val.network;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastService implements Runnable {

    private MulticastSocket socket;
    private final NetworkMain networkMain;

    private static final int BUFFER_SIZE = 1024;

    public MulticastService(NetworkMain networkMain) {
        this.networkMain = networkMain;

        try {
            InetAddress group = InetAddress.getByName("239.192.0.4");
            this.socket = new MulticastSocket(9192);
            socket.joinGroup(group);

            System.out.println("joined multicast group!");

        } catch (Exception e ) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        while(Thread.currentThread().isAlive()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(packet);
                networkMain.readMessage(packet);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

        }
    }
}
