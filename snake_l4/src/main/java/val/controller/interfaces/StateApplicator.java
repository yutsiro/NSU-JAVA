package val.controller.interfaces;

import me.ippolitov.fit.snakes.SnakesProto;

public interface StateApplicator {
    void getState(SnakesProto.GameState state);
}
