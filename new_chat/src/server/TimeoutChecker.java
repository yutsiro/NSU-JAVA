package server;

import java.util.ArrayList;
import java.util.List;

public class TimeoutChecker implements Runnable {
    private final ChatServer server;
    private final long timeout;

    public TimeoutChecker(ChatServer server, long timeout) {
        this.server = server;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        while (true) {
            try {
                long currentTime = System.currentTimeMillis();
                List<ClientHandler> toDisconnect = new ArrayList<>();
                synchronized (server.getClients()) {
                    for (ClientHandler client : server.getClients()) {
                        if (currentTime - client.getLastActivityTime() > timeout) {
                            toDisconnect.add(client);
                        }
                    }
                }
                for (ClientHandler client : toDisconnect) {
                    server.log("Клиент " + client.getClientName() + " отключён по таймауту");
                    client.disconnect();
                }
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                server.log("Ошибка в TimeoutChecker: " + e.getMessage());
            }
        }
    }
}