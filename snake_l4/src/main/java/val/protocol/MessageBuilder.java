package val.protocol;

import me.ippolitov.fit.snakes.SnakesProto;
import val.controller.GameContext;

public class MessageBuilder {

    private final GameContext context;

    public MessageBuilder(GameContext context) {
        this.context = context;
    }

    public SnakesProto.GameMessage buildAnnounce() {

        SnakesProto.GameAnnouncement announcement = SnakesProto.GameAnnouncement.newBuilder()
                .setConfig(buildConfig())
                .setGameName(context.getGameName())
                .setCanJoin(context.isCanJoin())
                .setPlayers(buildGamePlayers())
                .build();
        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(System.currentTimeMillis())
                .setAnnouncement(
                        SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                                .addGames(announcement)
                                .build()
                ).build();
    }

    public SnakesProto.GamePlayers buildGamePlayers() {

        SnakesProto.GamePlayers.Builder builder = SnakesProto.GamePlayers.newBuilder();

        for (PlayerInfo player : context.getPlayersMap().values()) {
            SnakesProto.GamePlayer.Builder gamePlayerBuilder = SnakesProto.GamePlayer.newBuilder();
            gamePlayerBuilder
                    .setName(player.getName())
                    .setId(player.getId())
                    .setRole(player.getRole())
                    .setType(player.getType())
                    .setScore(player.getScore());

            if (player.hasIpInfo()) {
                gamePlayerBuilder
                        .setIpAddress(player.getIp())
                        .setPort(player.getPort());
            }

            builder.addPlayers(gamePlayerBuilder.build());
        }

        return builder.build();
    }

    private SnakesProto.GameConfig buildConfig() {
        return SnakesProto.GameConfig.newBuilder()
                .setWidth(context.getWidth())
                .setHeight(context.getHeight())
                .setFoodStatic(context.getFoodStatic())
                .setStateDelayMs(context.getDelay())
                .build();
    }

    public SnakesProto.GameMessage buildJoinMsg(GameInfo info, boolean isViewer) {
        SnakesProto.GameMessage.JoinMsg.Builder builder = SnakesProto.GameMessage.JoinMsg.newBuilder();
        builder.setPlayerType(SnakesProto.PlayerType.HUMAN)
                .setPlayerName(context.getPlayerName())
                .setGameName(info.getGameSpecs().getGameName())
                .setRequestedRole(isViewer ? SnakesProto.NodeRole.VIEWER : SnakesProto.NodeRole.NORMAL);

        return SnakesProto.GameMessage.newBuilder()
                .setJoin(builder.build())
                .setMsgSeq(System.currentTimeMillis())
                .build();
    }

    public SnakesProto.GameMessage buildError(String reason) {
        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(System.currentTimeMillis())
                .setError(
                        SnakesProto.GameMessage.ErrorMsg.newBuilder()
                                .setErrorMessage(String.format("[%s] %s", context.getGameName(), reason))
                                .build()
                ).build();
    }

    public SnakesProto.GameMessage buildAck(int senderId, int receiverId, long seq){
        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(seq)
                .setReceiverId(receiverId)
                .setSenderId(senderId)
                .setAck(
                        SnakesProto.GameMessage.AckMsg.newBuilder().build()
                ).build();
    }

    public SnakesProto.GameMessage buildStateMsg(SnakesProto.GameState state) {
        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(System.currentTimeMillis())
                .setState(
                        SnakesProto.GameMessage.StateMsg.newBuilder()
                                .setState(state)
                                .build()
                ).build();
    }

    public SnakesProto.GameMessage buildRoleChange(SnakesProto.NodeRole senderRole, int senderId,
                                                   SnakesProto.NodeRole receiverRole, int receiverId){

        return SnakesProto.GameMessage.newBuilder()
                .setMsgSeq(System.currentTimeMillis())
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .setRoleChange(
                        SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                .setReceiverRole(receiverRole)
                                .setSenderRole(senderRole)
                                .build()
                ).build();

    }
}
