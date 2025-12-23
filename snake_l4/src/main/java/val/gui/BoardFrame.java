package val.gui;

import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto;
import val.controller.TouchAdapter;
import val.gui.interfaces.BoardView;

import javax.swing.*;
import java.awt.*;

public class BoardFrame extends JFrame implements BoardView {

    private StatsPanel statsPanel;
    private BoardMainPanel gamePanel;
    private JScrollPane gameScrollPane;

    @Setter
    private int id;

    public BoardFrame(SnakesProto.GameConfig config, TouchAdapter adapter, int id) {
        this.gamePanel = new BoardMainPanel(config.getWidth(), config.getHeight());
        this.statsPanel = new StatsPanel(gamePanel);
        this.id = id;

        this.setTitle("Snake Game - Player " + id);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBackground(new Color(245, 240, 255));

        gamePanel.addKeyListener(adapter);

        gameScrollPane = new JScrollPane(gamePanel);
        gameScrollPane.setPreferredSize(new Dimension(
                gamePanel.getGridWidth() * gamePanel.getCellSize() + 30,
                gamePanel.getGridHeight() * gamePanel.getCellSize() + 30
        ));
        gameScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 190, 220), 2),
                "Game Field (" + config.getWidth() + "×" + config.getHeight() + " cells)"
        ));

        gameScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gameScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        JPanel gameInfoPanel = new JPanel(new BorderLayout());
        gameInfoPanel.add(gameScrollPane, BorderLayout.CENTER);
        gameInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        contentPane.add(statsPanel, BorderLayout.WEST);
        contentPane.add(gameInfoPanel, BorderLayout.CENTER);

        int windowWidth = gamePanel.getGridWidth() * gamePanel.getCellSize() +
                /*statsPanel.getPreferredSize().width + */350;
        int windowHeight = Math.max(
                gamePanel.getGridHeight() * gamePanel.getCellSize() + 100,
                500
        );

        this.setMinimumSize(new Dimension(windowWidth, windowHeight));
        this.pack();

        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    @Override
    public void applyState(SnakesProto.GameState state) {
        statsPanel.setId(id);
        statsPanel.applyPlayersList(state.getPlayers().getPlayersList());
        gamePanel.applyScene(state);
    }
}