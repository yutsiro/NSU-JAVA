package val.gui;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BoardMainPanel extends JPanel {

    private static final int CELL_SIZE = 20;
    private static final String LOADING_MSG = "Waiting for game start...";
    @Getter
    private final int gridWidth;
    @Getter
    private final int gridHeight;

    private final Color BACKGROUND_COLOR = new Color(50, 50, 100);
    private final Color GRID_COLOR = new Color(235, 235, 235);
    private final Color FOOD_COLOR = new Color(240, 120, 105);

    private final Color[] SNAKE_COLORS = {
            new Color(255, 209, 220),
            new Color(182, 215, 168),
            new Color(173, 216, 230),
            new Color(255, 228, 196),
            new Color(221, 160, 221),
            new Color(255, 255, 224),
            new Color(175, 238, 238),
            new Color(245, 222, 179),
            new Color(216, 191, 216),
            new Color(255, 182, 193)
    };

    public BoardMainPanel(int gridWidth, int gridHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;

        int panelWidth = gridWidth * CELL_SIZE + 2;
        int panelHeight = gridHeight * CELL_SIZE + 2;

        this.setPreferredSize(new Dimension(panelWidth, panelHeight));
        this.setMinimumSize(new Dimension(panelWidth, panelHeight));
        this.setMaximumSize(new Dimension(panelWidth, panelHeight));
        this.setSize(panelWidth, panelHeight);

        this.setBackground(BACKGROUND_COLOR);
        this.setFocusable(true);
        this.requestFocusInWindow();

        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(220, 220, 220))
        ));
    }

    private List<SnakesProto.GameState.Snake> snakes;
    private List<SnakesProto.GameState.Coord> foods;

    public void applyScene(SnakesProto.GameState state) {
        snakes = state.getSnakesList();
        foods = state.getFoodsList();
        this.repaint();
    }

    public Color getSnakeColor(int playerId) {
        return SNAKE_COLORS[playerId % SNAKE_COLORS.length];
    }


    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(180, 180, 180));
        g.drawRect(0, 0, gridWidth * CELL_SIZE - 1, gridHeight * CELL_SIZE - 1);

        g.setColor(new Color(250, 250, 250));
        g.fillRect(gridWidth * CELL_SIZE, 0,
                getWidth() - gridWidth * CELL_SIZE,
                getHeight());
        g.fillRect(0, gridHeight * CELL_SIZE,
                getWidth(),
                getHeight() - gridHeight * CELL_SIZE);

        paintGrid(g);

        if (snakes == null || foods == null) {
            paintLoadingScreen(g);
            return;
        }

        paintFoods(g);
        paintSnakes(g);
    }

    private void paintLoadingScreen(Graphics g) {
        g.setColor(new Color(150, 150, 150));
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        int strWidth = g.getFontMetrics().stringWidth(LOADING_MSG);
        int centerX = (gridWidth * CELL_SIZE - strWidth) / 2;
        int centerY = gridHeight * CELL_SIZE / 2;
        g.drawString(LOADING_MSG, centerX, centerY);
    }

    private void paintSnakes(Graphics g) {
        if (snakes == null) return;

        for (SnakesProto.GameState.Snake snake : snakes) {
            int playerId = snake.getPlayerId();
            Color snakeColor = SNAKE_COLORS[playerId % SNAKE_COLORS.length];
            Color borderColor = snakeColor.darker();

            g.setColor(snakeColor);

            int x = 0;
            int y = 0;
            boolean isFirstSegment = true;

            for (SnakesProto.GameState.Coord point : snake.getPointsList()) {
                x += point.getX();
                y += point.getY();

                if (x < 0) x += gridWidth;
                if (x >= gridWidth) x %= gridWidth;
                if (y < 0) y += gridHeight;
                if (y >= gridHeight) y %= gridHeight;

                int pixelX = x * CELL_SIZE;
                int pixelY = y * CELL_SIZE;

                g.fillRoundRect(pixelX, pixelY, CELL_SIZE, CELL_SIZE, 6, 6);
                g.setColor(borderColor);
                g.drawRoundRect(pixelX, pixelY, CELL_SIZE, CELL_SIZE, 6, 6);
                g.setColor(snakeColor);

                if (isFirstSegment) {
                    g.setColor(snakeColor.darker());
                    g.fillRoundRect(pixelX, pixelY, CELL_SIZE, CELL_SIZE, 6, 6);
                    g.setColor(borderColor.darker());
                    g.drawRoundRect(pixelX, pixelY, CELL_SIZE, CELL_SIZE, 6, 6);
                    g.setColor(snakeColor);
                    isFirstSegment = false;
                }
            }
        }
    }

    private void paintFoods(Graphics g) {
        if (foods == null) return;

        g.setColor(FOOD_COLOR);

        for (SnakesProto.GameState.Coord food : foods) {
            int pixelX = food.getX() * CELL_SIZE + CELL_SIZE/4;
            int pixelY = food.getY() * CELL_SIZE + CELL_SIZE/4;
            int foodSize = CELL_SIZE/2;

            g.fillOval(pixelX, pixelY, foodSize, foodSize);
            g.setColor(FOOD_COLOR.darker());
            g.drawOval(pixelX, pixelY, foodSize, foodSize);
            g.setColor(FOOD_COLOR);
        }
    }

    private void paintGrid(Graphics g) {
        g.setColor(GRID_COLOR);
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                int centerX = x * CELL_SIZE;
                int centerY = y * CELL_SIZE;
                g.drawLine(centerX - 2, centerY, centerX + 2, centerY);
                g.drawLine(centerX, centerY - 2, centerX, centerY + 2);
            }
        }
    }

    public int getCellSize() {
        return CELL_SIZE;
    }
}