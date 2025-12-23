package val.controller;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto;
import val.controller.interfaces.StateApplicator;
import val.controller.interfaces.StateConstructor;
import val.protocol.PlayerInfo;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GameCore implements StateConstructor {

    private final GameContext context;
    private final StateApplicator controller;

    private final Thread stateTimer;

    private HashMap<Integer, SnakesProto.GameState.Snake> snakesMap = new HashMap<>();
    private ArrayList<SnakesProto.GameState.Coord> foods = new ArrayList<>();

    @Setter
    private int stateCounter = 0;

    public GameCore(GameContext context, StateApplicator controller) {
        this.context = context;
        this.controller = controller;
        stateTimer =  new Thread(new StateTimer(this, context.getDelay()));
    }

    public void startCore() {
        move();

        System.out.println("first move!!!");

        stateTimer.start();
    }

    public void stopCore() {
        stateTimer.interrupt();
    }

    public SnakesProto.GameState generateState() {


        SnakesProto.GameState.Builder builder = SnakesProto.GameState.newBuilder();
        builder.setStateOrder(stateCounter);
        builder.addAllFoods(foods).addAllSnakes(snakesMap.values()).setPlayers(context.wrapPlayers());

        stateCounter++;
        return builder.build();
    }

    public boolean canJoin(SnakesProto.GameMessage.JoinMsg msg) {
        if (msg.getRequestedRole() == SnakesProto.NodeRole.VIEWER) return true;
        return context.isCanJoin();
    }

    public void addPlayer(PlayerInfo info) {
        context.addNewPlayer(info);
        if (info.getRole() != SnakesProto.NodeRole.VIEWER) {
            System.out.printf("new player: %s, id: %d%n", info.getName(), info.getId());
            initSnake(info);
        }
    }

    private void generateFood() {
        while (foods.size() < context.getFoodStatic() + alive) {
            SnakesProto.GameState.Coord food = findPlace();
            foods.add(food);
        }
    }

    ListMultimap<Integer, Integer> headCollideMap = ArrayListMultimap.create();
    ListMultimap<Integer, Point> takenTilesMap = ArrayListMultimap.create();
    HashSet<Integer> toKill = new HashSet<>();

    private int hashCoord(SnakesProto.GameState.Coord c) {
        return (new Point(c.getX(), c.getY())).hashCode();
    }

    private void checkCollision() {
        if (headCollideMap.isEmpty()) {
            countAlive();
            return;
        }

        for (int tile : headCollideMap.keySet()) {
            List<Integer> list = headCollideMap.get(tile);
            if (list.size() != 1) {
                System.out.println("gonna collide: " + list);
                toKill.addAll(list);
            }
        }

        for (SnakesProto.GameState.Snake snake : snakesMap.values()) {
            SnakesProto.GameState.Coord head = snake.getPoints(0);
            Point headPoint = new Point(head.getX(), head.getY());
            if (takenTilesMap.containsValue(headPoint)) {
                System.out.println("boom body!");
                toKill.add(snake.getPlayerId());
            }
        }

        countAlive();
    }

    public boolean checkAppleConsume(SnakesProto.GameState.Coord head, SnakesProto.GameState.Snake.Builder snake) {
        int x = head.getX();
        int y = head.getY();

        SnakesProto.GameState.Coord foodToRemove = null;

        for (SnakesProto.GameState.Coord food : foods) {
            if (x == food.getX() && food.getY() == y) {
                PlayerInfo p = context.getPlayersMap().get(snake.getPlayerId());
                if (p != null) p.incScore();
                foodToRemove = food;
                break;
            }
        }

        if (foodToRemove != null) {
            foods.remove(foodToRemove);
            return true;
        }

        generateFood();
        return false;
    }

    private void kill(int id) {
        System.out.println("RIP id "+id);
        System.out.println("inside the kill, snakes before del: " + snakesMap);

        SnakesProto.GameState.Snake dead = snakesMap.get(id);
        snakesMap.remove(id);

        System.out.println("removed snake! snakes after: "+snakesMap);

        int x = 0;
        int y = 0;

        for (SnakesProto.GameState.Coord point : dead.getPointsList()) {
            x += point.getX();
            y += point.getY();
            if (x < 0) x += context.getWidth();
            if (x >= context.getWidth()) x %= context.getWidth();
            if (y < 0) y += context.getHeight();
            if (y >= context.getHeight()) y %= context.getHeight();

            if (System.currentTimeMillis() % 2 == 0) {
                System.out.println("new food!!!!");
                foods.add(
                        SnakesProto.GameState.Coord.newBuilder()
                                .setX(x)
                                .setY(y)
                                .build()
                );
            }
        }

        System.out.println("adding food");
    }

    private void killAll(Set<Integer> killList) {
        if (killList.isEmpty()) return;
        System.out.println("gonna iterate killList");
        for (int i : killList) {
            kill(i);
        }
        killList.clear();
    }

    private final SnakesProto.Direction[] forbiddenMoves = new SnakesProto.Direction[]{
            SnakesProto.Direction.DOWN,
            SnakesProto.Direction.UP,
            SnakesProto.Direction.RIGHT,
            SnakesProto.Direction.LEFT
    };

    public void moveSnakes() {
        ArrayList<SnakesProto.GameState.Snake> snakesTmp = new ArrayList<>(snakesMap.values());
        snakesMap.clear();

        for (SnakesProto.GameState.Snake snake : snakesTmp) {

            List<SnakesProto.GameState.Coord> parts = snake.getPointsList();
            List<SnakesProto.GameState.Coord> partsNew = new ArrayList<>();

            SnakesProto.GameState.Coord head = parts.get(0);
            SnakesProto.GameState.Coord.Builder headNew = SnakesProto.GameState.Coord.newBuilder();
            SnakesProto.GameState.Coord.Builder neck = SnakesProto.GameState.Coord.newBuilder();
            int x = head.getX();
            int y = head.getY();
            int xx = x;
            int yy = y;

            SnakesProto.Direction direction;
            SnakesProto.Direction prevDirection = snake.getHeadDirection();
            if (steerMap.containsKey(snake.getPlayerId())) {
                direction = steerMap.get(snake.getPlayerId());
                if (forbiddenMoves[prevDirection.getNumber()-1] == direction) direction = prevDirection;
            }
            else direction = prevDirection;

            switch (direction){
                case UP -> {
                    if (y-1 < 0) y = context.getHeight() - 1; else y -= 1;
                    headNew.setX(x);
                    headNew.setY(y);

                    neck.setX(0);
                    neck.setY(1);
                }
                case DOWN -> {
                    if (y+1 >= context.getHeight()) y = 0; else y += 1;
                    headNew.setX(x);
                    headNew.setY(y);

                    neck.setX(0);
                    neck.setY(-1);
                }
                case LEFT -> {
                    if (x-1 < 0) x = context.getWidth() - 1; else x -= 1;
                    headNew.setX(x);
                    headNew.setY(y);

                    neck.setX(1);
                    neck.setY(0);
                }
                case RIGHT -> {
                    if (x+1 >= context.getWidth()) x = 0; else x += 1;
                    headNew.setX(x);
                    headNew.setY(y);

                    neck.setX(-1);
                    neck.setY(0);
                }
            }
            partsNew.add(headNew.build());
            partsNew.add(neck.build());

            SnakesProto.GameState.Coord tail =  parts.get(parts.size()-1);

            headCollideMap.put(hashCoord(headNew.build()), snake.getPlayerId());
            takenTilesMap.put(snake.getPlayerId(), new Point(xx, yy));

            int i;
            for (i = 1; i != parts.size()-1; i++) {
                SnakesProto.GameState.Coord part = parts.get(i);
                partsNew.add(part);
                xx += part.getX();
                yy += part.getY();
                takenTilesMap.put(snake.getPlayerId(), new Point(xx, yy));
            }

            SnakesProto.GameState.Snake.Builder sb = snake.toBuilder();

            boolean isNeedToEnlarge = checkAppleConsume(snake.getPoints(0), sb);
            if (isNeedToEnlarge) {
                partsNew.add(tail);
            }

            snakesMap.put(sb.getPlayerId(), sb.setHeadDirection(direction).clearPoints().addAllPoints(partsNew).build());
        }


    }

    private void move() {
        checkCollision();
        killAll(toKill);

        headCollideMap.clear();
        takenTilesMap.clear();

        moveSnakes();
    }

    private SnakesProto.GameState.Coord findPlace() {
        int x, y;
        Point p;
        do {
            x = ThreadLocalRandom.current().nextInt(1, context.getWidth());
            y = ThreadLocalRandom.current().nextInt(1, context.getHeight());
            p = new Point(x, y);
        } while (takenTilesMap.containsValue(p));

        return SnakesProto.GameState.Coord.newBuilder()
                .setX(x).setY(y).build();
    }

    private void initSnake(PlayerInfo info) {
        SnakesProto.GameState.Coord head = findPlace();
        SnakesProto.GameState.Snake snake = SnakesProto.GameState.Snake.newBuilder()
                .setHeadDirection(SnakesProto.Direction.RIGHT)
                .setState(SnakesProto.GameState.Snake.SnakeState.ALIVE)
                .setPlayerId(info.getId())
                .addPoints(head)
                .addPoints(SnakesProto.GameState.Coord.newBuilder()
                        .setX(-1).setY(0).build())
                .build();

        snakesMap.put(snake.getPlayerId(), snake);

    }

    private int alive = 0;
    private void countAlive() {
        int i = 0;
        for (SnakesProto.GameState.Snake snake : snakesMap.values())
            if (snake.getState() == SnakesProto.GameState.Snake.SnakeState.ALIVE) i++;
        alive = i;
    }

    @Override
    public void releaseFreshState() {
        controller.getState(generateState());
        move();
    }

    private final HashMap<Integer, SnakesProto.Direction> steerMap = new HashMap<>();

    public void applySteer(SnakesProto.GameMessage m) {
        steerMap.put(m.getSenderId(), m.getSteer().getDirection());
    }

    public void getListsFromState(SnakesProto.GameState state) {
        snakesMap.clear();
        foods.clear();

        foods = new ArrayList<>(state.getFoodsList());

        for (SnakesProto.GameState.Snake snake : state.getSnakesList()) {
            snakesMap.put(snake.getPlayerId(), snake);
        }
    }
}