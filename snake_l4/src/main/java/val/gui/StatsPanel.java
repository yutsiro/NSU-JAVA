package val.gui;

import lombok.Setter;
import me.ippolitov.fit.snakes.SnakesProto;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class StatsPanel extends JPanel {

    JPanel scoreboardPanel;
    @Setter
    int id;

    private final Color PANEL_BACKGROUND = new Color(250, 245, 255);
    private final Color BORDER_COLOR = new Color(200, 190, 220);
    private final Color TEXT_COLOR = new Color(80, 70, 100);
    private BoardMainPanel boardPanel;

    private final Color[] PLAYER_COLORS = {
            new Color(255, 230, 230),
            new Color(230, 255, 230),
            new Color(230, 230, 255),
            new Color(255, 255, 230),
            new Color(255, 230, 255),
            new Color(230, 255, 255),
            new Color(255, 240, 230),
            new Color(240, 255, 240),
            new Color(240, 240, 255),
            new Color(255, 250, 230)
    };

    public StatsPanel(BoardMainPanel boardPanel) {
        this.boardPanel = boardPanel;
        this.scoreboardPanel = new JPanel();
        scoreboardPanel.setPreferredSize(new Dimension(250, 400));
        scoreboardPanel.setMinimumSize(new Dimension(250, 400));
        scoreboardPanel.setMaximumSize(new Dimension(250, 400));
        scoreboardPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 2, true),
                new EmptyBorder(10, 10, 10, 10)
        ));
        scoreboardPanel.setBackground(PANEL_BACKGROUND);
        scoreboardPanel.setFocusable(false);

        initScoreboard();

        JLabel title = new JLabel("Scoreboard");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(TEXT_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(title);
        this.add(Box.createRigidArea(new Dimension(0, 10)));
        this.add(scoreboardPanel);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setFocusable(false);
        this.setBackground(PANEL_BACKGROUND);

        this.setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    private JList<String> playersList;
    private void initScoreboard() {
        playersList = new JList<>();
        playersList.setFocusable(false);
        playersList.setFixedCellWidth(250);
        playersList.setFixedCellHeight(28);
        playersList.setFont(new Font("Arial", Font.PLAIN, 13));
        playersList.setCellRenderer(new NewListRenderer());
        playersList.setBackground(PANEL_BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(playersList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(PANEL_BACKGROUND);

        scoreboardPanel.setLayout(new BorderLayout());
        scoreboardPanel.add(scrollPane, BorderLayout.CENTER);
    }

    int[] colorsArr = new int[50];
    public void applyPlayersList(List<SnakesProto.GamePlayer> playerList) {
        String[] players = new String[playerList.size()];
        int i = 0;
        List<SnakesProto.GamePlayer> tmp = new ArrayList<>(playerList);
        tmp.sort(Comparator.comparingInt(SnakesProto.GamePlayer::getScore).reversed());

        for (SnakesProto.GamePlayer player : tmp) {
            String roleIcon = "";
            switch (player.getRole()) {
                case MASTER -> roleIcon = "M ";
                case DEPUTY -> roleIcon = "D ";
                case NORMAL -> roleIcon = "- ";
                case VIEWER -> roleIcon = " ";
            }

            String playerText = String.format("%s %-15s %3d pts",
                    roleIcon,
                    truncate(player.getName(), 12),
                    player.getScore()
            );

            if (id == player.getId()) {
                playerText = playerText + " (YOU)";
            }

            players[i] = playerText;
            colorsArr[i] = player.getId();
            i++;
        }
        playersList.setListData(players);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private class NewListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
            );

            label.setText(value.toString());

            label.setOpaque(true);

            Color snakeColor;
            if (boardPanel != null) {
                snakeColor = boardPanel.getSnakeColor(colorsArr[index]);
            } else {
                snakeColor = PLAYER_COLORS[colorsArr[index] % PLAYER_COLORS.length];
            }

            Color backgroundColor = new Color(
                    Math.min(255, snakeColor.getRed() + 40),
                    Math.min(255, snakeColor.getGreen() + 40),
                    Math.min(255, snakeColor.getBlue() + 40)
            );

            Color borderColor = snakeColor.darker();

            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, 1),
                    new EmptyBorder(5, 10, 5, 10)
            ));

            if (isSelected) {
                label.setBackground(backgroundColor.brighter());
            } else {
                label.setBackground(backgroundColor);
            }

            if (isDarkColor(backgroundColor)) {
                label.setForeground(Color.WHITE);
            } else {
                label.setForeground(TEXT_COLOR);
            }

            return label;
        }

        private boolean isDarkColor(Color color) {
            double brightness = (color.getRed() * 0.299 +
                    color.getGreen() * 0.587 +
                    color.getBlue() * 0.114);
            return brightness < 128;
        }
    }
}