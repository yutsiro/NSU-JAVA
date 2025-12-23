package val.controller;

import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto;
import val.controller.interfaces.Controller;
import val.controller.interfaces.StateApplicator;
import val.gui.BoardFrame;
import val.gui.interfaces.BoardView;
import val.gui.interfaces.ControlsView;
import val.network.NetworkMain;
import val.protocol.GameInfo;
import val.protocol.MessageBuilder;
import val.protocol.PlayerInfo;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;

public class MainController implements Controller, StateApplicator {

    private final GameContext context;
    private final NetworkMain networkMain;
    private final MessageBuilder messageBuilder;
    private GameCore gameCore;
    private TouchAdapter adapter;

    @Setter
    private ControlsView controls;
    private BoardView board;

    private int id;

    private InetAddress masterAddress;
    private int masterPort;
    private int masterId;

    private final HashSet<GameInfo> games = new HashSet<>();

    private SnakesProto.NodeRole currentRole;

    private long lastAnnMsgId = 0;

    private Thread checkThread;
    private MessageRecvTimer timer;

    public MainController(GameContext context) {
        this.context = context;
        this.networkMain = new NetworkMain(this);
        this.messageBuilder = new MessageBuilder(this.context);
        this.adapter = new TouchAdapter(this);

        id = (int)(System.currentTimeMillis() % 1000000);
        System.out.println("current client id: " + id);

        timer = new MessageRecvTimer(this, networkMain, context);
        checkThread = new Thread(timer);
    }

    public void stopGame() {
        if (currentRole != SnakesProto.NodeRole.MASTER) {
            currentRole = SnakesProto.NodeRole.VIEWER;
            networkMain.sendMessage(messageBuilder.buildRoleChange(
                    SnakesProto.NodeRole.VIEWER, id, SnakesProto.NodeRole.MASTER, masterId
            ), masterAddress, masterPort);
            return;
        }
        networkMain.stopAnnounce();

        PlayerInfo dep = context.findRole(SnakesProto.NodeRole.DEPUTY);
        System.out.println("found dep:" + dep);
        if (dep != null) {
            System.out.println("tell about it to deputy!");
            networkMain.sendMessage(
                    messageBuilder.buildRoleChange(SnakesProto.NodeRole.VIEWER, id, SnakesProto.NodeRole.MASTER, dep.getId()),
                    dep.getInetAddress(), dep.getPort()
            );
        }
        currentRole = SnakesProto.NodeRole.VIEWER;

        gameCore.stopCore();

        isGameOn = false;

    }

    int lastState = 0;
    boolean ind = false;

    @Override
    public void deliverMessage(DatagramPacket packet) {
        int len = packet.getLength();

        byte[] data = Arrays.copyOfRange(packet.getData(), 0, len);

        try {
            SnakesProto.GameMessage message = SnakesProto.GameMessage.parseFrom(data);
            if (message.getMsgSeq() == lastAnnMsgId) return;

            switch (message.getTypeCase()) {
                case ANNOUNCEMENT -> {
                    GameInfo newGame =
                            new GameInfo(packet.getAddress(), packet.getPort(), message.getAnnouncement().getGames(0));
                    games.add(newGame);
                    controls.updateGames(games);
                }
                case JOIN -> {
                    System.out.println(message.getJoin().getPlayerName() +
                            " trying to join as "+message.getJoin().getRequestedRole());

                    if (!gameCore.canJoin(message.getJoin())) {
                        networkMain.sendMessage(messageBuilder.buildError("Game is full or cannot join"),
                                packet.getAddress(), packet.getPort());
                        return;
                    }

                    int newId = (int)(System.currentTimeMillis() % 1000000);
                    SnakesProto.NodeRole role = rolePicker(message.getJoin().getRequestedRole());

                    if (role == SnakesProto.NodeRole.DEPUTY) {
                        networkMain.sendMessage(messageBuilder.buildRoleChange(
                                SnakesProto.NodeRole.MASTER, id, role, newId
                        ), packet.getAddress(), packet.getPort());
                    }

                    networkMain.sendMessage(
                            messageBuilder.buildAck(id, newId, message.getMsgSeq()),
                            packet.getAddress(), packet.getPort()
                    );

                    if (currentRole == SnakesProto.NodeRole.MASTER) {
                        SnakesProto.GameState currentState = gameCore.generateState();
                        SnakesProto.GameMessage stateMsg = messageBuilder.buildStateMsg(currentState);
                        networkMain.sendMessage(stateMsg, packet.getAddress(), packet.getPort());
                        System.out.println("[DEBUG] Sent initial state to new player " + newId);
                    }

                    timer.saveIp(packet.getAddress(), packet.getPort(), newId);
                    timer.timeStamp(newId);
                }
                case ERROR -> {
                    if (message.getError().getErrorMessage().equals("[test] master death")) becomeMaster();
                }
                case ACK -> {
                    if (currentRole != SnakesProto.NodeRole.MASTER && !ind) {
                        ind = true;
                        this.id = message.getReceiverId();
                        this.masterId = message.getSenderId();
                        System.out.println("id now: " + this.id);
                        this.timer.setCurId(message.getReceiverId());
                        this.timer.setMasterId(this.masterId);
                        this.board.setId(id);
                    }

                    this.timer.timeStamp(message.getSenderId());
                }
                case STATE -> {
                    if (lastState >= message.getState().getState().getStateOrder() && lastStateMessage != null) break;

                    board.applyState(message.getState().getState());
                    lastStateMessage = message;

                    lastState = message.getState().getState().getStateOrder();
                    this.timer.setMasterId(this.masterId);
                    this.timer.timeStamp(this.masterId);
                }
                case PING -> {
                    this.networkMain.sendMessage(this.messageBuilder.buildAck(this.id, message.getSenderId(), message.getMsgSeq()), packet.getAddress(), packet.getPort());
                    this.timer.timeStampIp(new InetInfo(packet.getAddress(), packet.getPort()));
                }
                case STEER -> {
                    deliverSteer(message);
                    networkMain.sendMessage(
                            messageBuilder.buildAck(id, message.getSenderId(), message.getMsgSeq()), packet.getAddress(), packet.getPort()
                    );
                    this.timer.timeStamp(message.getSenderId());
                }
                case ROLE_CHANGE -> {
                    roleProceed(message, packet.getAddress(), packet.getPort());

                }
                case DISCOVER -> networkMain.sendMessage(messageBuilder.buildAnnounce(), packet.getAddress(), packet.getPort());
                case TYPE_NOT_SET -> System.out.println("Message type was not set!");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void roleProceed(SnakesProto.GameMessage message, InetAddress a, int port) {
        SnakesProto.GameMessage.RoleChangeMsg m = message.getRoleChange();
        switch (m.getSenderRole()) {
            case NORMAL -> {/*do nothing*/}
            case MASTER -> {
                masterAddress = a;
                masterPort = port;
                masterId = message.getSenderId();
                if (m.getSenderRole() == SnakesProto.NodeRole.VIEWER) {
                    System.out.println("you ded");
                    context.changeRole(message.getReceiverId(), SnakesProto.NodeRole.VIEWER);
                }
                if (m.getReceiverRole() == SnakesProto.NodeRole.DEPUTY) {
                    currentRole = SnakesProto.NodeRole.DEPUTY;
                    System.out.println("now im deputy!!!");
                }
            }
            case DEPUTY -> {/*do nothing1*/}
            case VIEWER -> {
                if (m.getReceiverRole() == SnakesProto.NodeRole.DEPUTY) {
                    currentRole = SnakesProto.NodeRole.DEPUTY;
                    System.out.println("now im deputy!!!");
                } else if (m.getReceiverRole() == SnakesProto.NodeRole.MASTER) {
                    if (currentRole != SnakesProto.NodeRole.MASTER) {
                        System.out.println("im gonna be master!!!");
                        becomeMaster();
                    }
                }
                context.changeRole(message.getSenderId(), SnakesProto.NodeRole.VIEWER);
                System.out.println("!!!!!!!!!!" + context.getPlayersMap().get(message.getSenderId()).getRole());
            }
        }
        System.out.println(context.getPlayersMap());
    }

    @Override
    public void getState(SnakesProto.GameState state) {
        SnakesProto.GameMessage stateMsg = messageBuilder.buildStateMsg(state);

        if (currentRole != SnakesProto.NodeRole.MASTER) return;
        for (PlayerInfo player : context.getPlayersMap().values()) {
            if (player.getRole() == SnakesProto.NodeRole.MASTER) continue;
            if (player.hasIpInfo()) networkMain.sendMessage(stateMsg,
                    player.getInetAddress(), player.getPort());
        }
        board.applyState(state);
    }

    private boolean isGameOn;

    public void startGame(String playerName) {
        if (isGameOn) return;
        isGameOn = true;
        currentRole = SnakesProto.NodeRole.MASTER;

        PlayerInfo m = new PlayerInfo(
                playerName,
                id, SnakesProto.NodeRole.MASTER,
                SnakesProto.PlayerType.HUMAN, 0);

        gameCore = new GameCore(context, this);

        context.addNewPlayer(m);
        gameCore.addPlayer(m);
        gameCore.startCore();

        SnakesProto.GameMessage ann = messageBuilder.buildAnnounce();
        lastAnnMsgId = ann.getMsgSeq();
        networkMain.startAnnounce(ann);


        board = new BoardFrame(ann.getAnnouncement().getGames(0).getConfig(), adapter, id);

        ind = true;
        timer.setCurId(id);
        checkThread.start();
    }

    private void deliverSteer(SnakesProto.GameMessage m) {
        gameCore.applySteer(m);
    }

    public SnakesProto.NodeRole rolePicker(SnakesProto.NodeRole requestedRole) {
        if (requestedRole == SnakesProto.NodeRole.VIEWER) return SnakesProto.NodeRole.VIEWER;

        if (requestedRole == SnakesProto.NodeRole.NORMAL && context.findRole(SnakesProto.NodeRole.DEPUTY) == null) {
            return SnakesProto.NodeRole.DEPUTY;
        }

        return SnakesProto.NodeRole.NORMAL;
    }

    public void connect(GameInfo info, boolean isViewer) {
        if (info == null) return;

        ind = false;
        board = new BoardFrame(info.getGameSpecs().getConfig(), adapter, id);
        System.out.println("try to connect...");
        networkMain.sendMessage(messageBuilder.buildJoinMsg(info, isViewer), info.getAddr(), info.getPort());

        masterAddress = info.getAddr();
        masterPort = info.getPort();

        context.getFromGameInfo(info);

        this.timer.setMasterInfo(this.masterAddress, this.masterPort);
        this.checkThread.start();
    }

    public void applySteerMsg(SnakesProto.Direction d) {
        SnakesProto.GameMessage m = SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(System.currentTimeMillis())
                .setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder()
                        .setDirection(d)
                        .build())
                .setSenderId(id)
                .build();

        if (currentRole == SnakesProto.NodeRole.MASTER) {
            deliverSteer(m);
            return;
        }
        networkMain.sendMessage(m, masterAddress, masterPort);
    }

    private SnakesProto.GameMessage lastStateMessage;

    public void becomeMaster() {
        currentRole = SnakesProto.NodeRole.MASTER;

        for (PlayerInfo player : context.getPlayersMap().values()) {
            if (player.getRole() == SnakesProto.NodeRole.VIEWER) continue;
            if (player.getId() == id) continue;
            networkMain.sendMessage(messageBuilder.buildRoleChange(
                    SnakesProto.NodeRole.MASTER, id, SnakesProto.NodeRole.NORMAL, player.getId()
            ), player.getInetAddress(), player.getPort());
        }

        SnakesProto.GameState state = lastStateMessage.getState().getState();
        int stateOrder = state.getStateOrder();

        context.getFromGameState(state);

        gameCore = new GameCore(context, this);

        SnakesProto.GameMessage ann = messageBuilder.buildAnnounce();

        lastAnnMsgId = ann.getMsgSeq();
        networkMain.startAnnounce(ann);

        gameCore.getListsFromState(state);

        gameCore.setStateCounter(stateOrder);

        gameCore.startCore();

        PlayerInfo lastMaster = context.getPlayersMap().get(masterId);
        lastMaster.setIp(masterAddress.toString());
        lastMaster.setPort(masterPort);
        lastMaster.setRole(SnakesProto.NodeRole.VIEWER);
        context.addNewPlayer(lastMaster);

        findDeputy();
    }

    private void findDeputy() {
        for (PlayerInfo player : context.getPlayersMap().values()) {
            if (player.hasIpInfo() && player.getRole() == SnakesProto.NodeRole.NORMAL) {
                player.setRole(SnakesProto.NodeRole.DEPUTY);
                context.addNewPlayer(player);
                networkMain.sendMessage(
                        messageBuilder.buildRoleChange(SnakesProto.NodeRole.MASTER, id, SnakesProto.NodeRole.DEPUTY, player.getId()),
                        player.getInetAddress(), player.getPort()
                );
                return;
            }
        }
    }

    public void proceedDeath(int deadId) {
        if (this.currentRole != SnakesProto.NodeRole.MASTER) {
            this.context.getFromGameState(this.lastStateMessage.getState().getState());
        }

        if (!this.context.getPlayersMap().containsKey(deadId)) {
            System.out.println("no such player with id " + deadId);
            System.out.println("players map : " + this.context.getPlayersMap());
        } else {
            System.out.println("this player role: " + this.currentRole);
            if (deadId == this.masterId && this.currentRole == SnakesProto.NodeRole.DEPUTY) {
                System.out.println("!!!!!!!!!!!!!!!!!!!!! master is dead!!");
                this.becomeMaster();
            }

            this.context.removePlayer(deadId);
        }
    }

}
