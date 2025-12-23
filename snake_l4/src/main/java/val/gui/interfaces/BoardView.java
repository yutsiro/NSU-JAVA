package val.gui.interfaces;

import me.ippolitov.fit.snakes.SnakesProto;

public interface BoardView {
    void applyState(SnakesProto.GameState state);
    void setId(int id);
}
