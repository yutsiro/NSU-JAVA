package val.protocol;

import lombok.Getter;
import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;

@Getter
@Setter
public class GameInfo {

    private InetAddress addr;
    private int port;
    private me.ippolitov.fit.snakes.SnakesProto.GameAnnouncement gameSpecs;
    private int aliveCounter;

    public GameInfo(InetAddress addr, int port, SnakesProto.GameAnnouncement gameSpecs) {
        this.addr = addr;
        this.port = port;
        this.gameSpecs = gameSpecs;

        aliveCounter = 0;
        for (SnakesProto.GamePlayer p : gameSpecs.getPlayers().getPlayersList()) {
            if (p.getRole() != SnakesProto.NodeRole.VIEWER) aliveCounter++;
        }

    }

    @Override
    public int hashCode() {
        return gameSpecs.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != GameInfo.class) return false;
        return obj.hashCode() == this.hashCode();
    }

    @Override
    public String toString() {
        return String.format(
                "[%s:%d] %s, %dx%d",
                addr, port, gameSpecs.getGameName(), gameSpecs.getConfig().getHeight(),
                gameSpecs.getConfig().getWidth()
        );
    }
}
