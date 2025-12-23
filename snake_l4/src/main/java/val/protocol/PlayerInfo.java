package val.protocol;

import lombok.Getter;
import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;

@Getter
@Setter
public class PlayerInfo {
    private String name;
    private int id;
    private String ip;
    private int port;
    @Setter
    private SnakesProto.NodeRole role;
    private SnakesProto.PlayerType type;
    private int score;

    public PlayerInfo(String name, int id, String ip, int port,
                      SnakesProto.NodeRole role, SnakesProto.PlayerType type, int score) {
        this.name = name;
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.role = role;
        this.type = type;
        this.score = score;
    }

    public PlayerInfo(String name, int id, SnakesProto.NodeRole role,
                      SnakesProto.PlayerType type, int score) {
        this.name = name;
        this.id = id;
        this.role = role;
        this.type = type;
        this.score = score;
        this.ip = null;
        this.port = -1;
    }

    public void incScore() {
        score++;
    }

    public boolean hasIpInfo() {
        return (ip != null && !ip.isEmpty()) && (port > 0 && port <= 65535);
    }

    public InetAddress getInetAddress() {
        if (ip == null || ip.isEmpty() || port <= 0) {
            return null;
        }
        try {
            String cleanIp = ip.startsWith("/") ? ip.substring(1) : ip;
            return InetAddress.getByName(cleanIp);
        } catch (Exception e) {
            System.out.println("Cannot resolve address: " + ip + ":" + port + " - " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return id + ": " + name + " " + role + (hasIpInfo() ? " [" + ip + ":" + port + "]" : "");
    }

}