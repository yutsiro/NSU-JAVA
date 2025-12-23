package val.gui.interfaces;

import val.protocol.GameInfo;

import java.util.HashSet;

public interface ControlsView {
    void updateGames(HashSet<GameInfo> games);
    String getPlayerName();
}
