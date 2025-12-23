package val.controller;

import lombok.Getter;
import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto;
import val.protocol.GameInfo;
import val.protocol.PlayerInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Getter
@Setter
public class GameContext {

    private final HashMap<String, String> configs = new HashMap<>();
    private final HashMap<Integer, PlayerInfo> playersMap = new HashMap<>();

    private int height;
    private int width;
    private int foodStatic;
    private String gameName;
    private int delay;
    private boolean canJoin;
    private String playerName;

    public GameContext() {
        parseConfig();
    }

    public void parseConfig() {
        try {
            InputStream is = ClassLoader.getSystemResourceAsStream("config");
            assert is != null;
            Scanner scanner = new Scanner(is);
            while (scanner.hasNext()) {
                String[] line = scanner.nextLine().split("=");
                configs.put(line[0], line[1]);
            }
            scanner.close();
            is.close();

            playersMap.clear();
            height = Integer.parseInt(configs.get("height"));
            width = Integer.parseInt(configs.get("width"));
            foodStatic = Integer.parseInt(configs.get("food_static"));
            gameName = configs.get("name");
            delay = Integer.parseInt(configs.get("delay"));
            canJoin = Boolean.parseBoolean(configs.get("can_join"));
            playerName = configs.get("player");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addNewPlayer(PlayerInfo playerInfo) {
        playersMap.put(playerInfo.getId(), playerInfo);
    }

    public SnakesProto.GamePlayers wrapPlayers() {
        ArrayList<SnakesProto.GamePlayer> playersTmp = new ArrayList<>();
        for (Map.Entry<Integer, PlayerInfo> playerEntry : playersMap.entrySet()) {
            PlayerInfo player = playerEntry.getValue();
            if (player.getRole() == SnakesProto.NodeRole.VIEWER ) continue;
            SnakesProto.GamePlayer.Builder builder = SnakesProto.GamePlayer.newBuilder()
                    .setName(player.getName())
                    .setId(playerEntry.getKey())
                    .setRole(player.getRole())
                    .setType(player.getType())
                    .setScore(player.getScore());
            if (player.hasIpInfo()) builder.setPort(player.getPort())
                                            .setIpAddress(player.getIp());

            playersTmp.add(builder.build());
        }
        return SnakesProto.GamePlayers.newBuilder().addAllPlayers(playersTmp).build();
    }

    public PlayerInfo findRole(SnakesProto.NodeRole role) {
        for (Map.Entry<Integer, PlayerInfo> player : playersMap.entrySet()) {
            if (player.getValue().getRole() == role) return player.getValue();
        }
        return null;
    }

    public void getFromGameInfo(GameInfo info) {
        SnakesProto.GameConfig data = info.getGameSpecs().getConfig();

        this.width = data.getWidth();
        this.height = data.getHeight();
        this.foodStatic = data.getFoodStatic();
        this.delay = data.getStateDelayMs();

        PlayerInfo infop;
        for(SnakesProto.GamePlayer player : info.getGameSpecs().getPlayers().getPlayersList()) {
            if (!player.hasPort() && !player.hasIpAddress()) {
                infop = new PlayerInfo(player.getName(), player.getId(), player.getRole() == SnakesProto.NodeRole.DEPUTY ? SnakesProto.NodeRole.MASTER : SnakesProto.NodeRole.NORMAL, player.getType(), player.getScore());
            } else {
                infop = new PlayerInfo(player.getName(), player.getId(), player.getIpAddress(), player.getPort(), player.getRole() == SnakesProto.NodeRole.DEPUTY ? SnakesProto.NodeRole.MASTER : SnakesProto.NodeRole.NORMAL, player.getType(), player.getScore());
            }
            playersMap.put(infop.getId(), infop);
        }


    }

    public void getFromGameState(SnakesProto.GameState state) {
        playersMap.clear();

        for (SnakesProto.GamePlayer player : state.getPlayers().getPlayersList()) {

            PlayerInfo info = new PlayerInfo(player.getName(), player.getId(), player.getIpAddress(), player.getPort(),
                    (player.getRole() == SnakesProto.NodeRole.DEPUTY ? SnakesProto.NodeRole.MASTER : SnakesProto.NodeRole.NORMAL),
                    player.getType() ,player.getScore());

            playersMap.put(player.getId(), info);
        }

        System.out.println("the player list: " + playersMap);
    }

    public void changeRole(int id, SnakesProto.NodeRole role) {
        playersMap.get(id).setRole(role);
    }

    public void removePlayer(int id) {
        this.playersMap.remove(id);
    }

}
