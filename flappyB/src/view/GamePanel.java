package view;

import model.Drawable;
import model.GameModel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GamePanel extends JPanel {
    private GameModel model;
    private final Font titleFont = new Font("Arial", Font.BOLD, 40);
    private final Font menuFont = new Font("Arial", Font.PLAIN, 20);

    public GamePanel() {
        setupHotkeys();
    }

    public void setModel(GameModel model) {
        this.model = model;
    }

    private void setupHotkeys() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("S"), "startGame");
        actionMap.put("startGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model != null) {
                    model.startGame();
                    repaint();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("R"), "resetGame");
        actionMap.put("resetGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model != null) {
                    model.reset();
                    repaint();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "exitGame");
        actionMap.put("exitGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model != null) {
                    model.exit();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (model != null) {
            //фон
            g.setColor(Color.CYAN);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (model.getGameState() == GameModel.GameState.MENU) {
                drawMenu(g, "Flappy Bird", Color.BLACK, false);
            } else if (model.getGameState() == GameModel.GameState.GAME_OVER) {
                drawMenu(g, "Game Over", Color.RED, true);
            } else {
                //все объекты
                for (Drawable drawable : model.getDrawables()) {
                    drawable.draw(g);
                }

                //счет
                g.setColor(Color.BLACK);
                g.setFont(menuFont);
                g.drawString("Score: " + model.getScore(), 10, 30);
            }
        }
    }

    private void drawMenu(Graphics g, String title, Color titleColor, boolean showScore) {
        g.setColor(titleColor);
        g.setFont(titleFont);
        g.drawString(title, 200, 200);
        g.setColor(Color.BLACK);
        g.setFont(menuFont);
        if (showScore) {
            g.drawString("Score: " + model.getScore(), 230, 250);
        }
        g.drawString("Press S to Start", 230, 300);
        g.drawString("Press R to Reset", 230, 330);
        g.drawString("Press Esc to Exit", 230, 360);
    }
}