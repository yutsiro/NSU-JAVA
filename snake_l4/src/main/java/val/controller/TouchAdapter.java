package val.controller;

import me.ippolitov.fit.snakes.SnakesProto;
import val.controller.interfaces.Controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class TouchAdapter extends KeyAdapter {

    private final Controller controller;
    private SnakesProto.Direction lastDirection = SnakesProto.Direction.RIGHT;

    public TouchAdapter(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        SnakesProto.Direction newDirection = lastDirection;

        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            newDirection = SnakesProto.Direction.LEFT;
        } else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            newDirection = SnakesProto.Direction.RIGHT;
        } else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            newDirection = SnakesProto.Direction.UP;
        } else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            newDirection = SnakesProto.Direction.DOWN;
        }

        if (!isOppositeDirection(lastDirection, newDirection)) {
            controller.applySteerMsg(newDirection);
            lastDirection = newDirection;
        }
    }

    private boolean isOppositeDirection(SnakesProto.Direction dir1, SnakesProto.Direction dir2) {
        return (dir1 == SnakesProto.Direction.LEFT && dir2 == SnakesProto.Direction.RIGHT) ||
                (dir1 == SnakesProto.Direction.RIGHT && dir2 == SnakesProto.Direction.LEFT) ||
                (dir1 == SnakesProto.Direction.UP && dir2 == SnakesProto.Direction.DOWN) ||
                (dir1 == SnakesProto.Direction.DOWN && dir2 == SnakesProto.Direction.UP);
    }
}