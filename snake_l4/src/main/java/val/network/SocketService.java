package val.network;

import lombok.AllArgsConstructor;
import val.controller.interfaces.Controller;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

@AllArgsConstructor
public class SocketService implements Runnable{

    private final DatagramSocket socket;
    private final Controller networkMain;

    @Override
    public void run() {
        int SIZE = 1024;

        while (Thread.currentThread().isAlive()) {
            DatagramPacket packet = new DatagramPacket(new byte[SIZE], SIZE);
            try {
                socket.receive(packet);
                networkMain.deliverMessage(packet);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

}
