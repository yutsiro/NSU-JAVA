package val.controller.interfaces;

import me.ippolitov.fit.snakes.SnakesProto;

import java.net.DatagramPacket;

public interface Controller {
    void deliverMessage(DatagramPacket message);
    void applySteerMsg(SnakesProto.Direction m);
    void proceedDeath(int id);
}
