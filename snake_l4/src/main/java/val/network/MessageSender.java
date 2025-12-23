package val.network;

import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;

public interface MessageSender {
    void sendMessage(me.ippolitov.fit.snakes.SnakesProto.GameMessage msg, InetAddress address, int port);
}
