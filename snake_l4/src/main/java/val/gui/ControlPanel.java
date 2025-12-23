package val.gui;

import val.controller.MainController;
import val.gui.interfaces.ControlsView;
import val.protocol.GameInfo;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.HashSet;

public class ControlPanel extends JPanel implements ControlsView {

    private JPanel buttons;
    private JPanel otherBoards;
    private JPanel descr;
    private JCheckBox isViewer = new JCheckBox();
    private JTextField playerNameField;

    private MainController masterController;

    private final Color PANEL_BACKGROUND = new Color(250, 245, 255);
    private final Color BORDER_COLOR = new Color(200, 190, 220);
    private final Color TEXT_COLOR = new Color(80, 70, 100);

    public ControlPanel(MainController controller) {
        this.masterController = controller;

        this.setPreferredSize(new Dimension(500, 500));
        this.setBackground(PANEL_BACKGROUND);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(15, 15, 15, 15));

        initButtonsPanel();
        initOtherBoardsPanel();
        initDescription();

        this.add(createSection("Game Controls", buttons));
        this.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel viewerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        viewerPanel.setBackground(PANEL_BACKGROUND);
        isViewer.setText("Connect as viewer");
        isViewer.setFont(new Font("Arial", Font.PLAIN, 14));
        isViewer.setForeground(TEXT_COLOR);
        isViewer.setBackground(PANEL_BACKGROUND);
        viewerPanel.add(isViewer);
        this.add(viewerPanel);
        this.add(Box.createRigidArea(new Dimension(0, 10)));

        this.add(createSection("Available Games", otherBoards));
        this.add(Box.createRigidArea(new Dimension(0, 15)));
        this.add(createSection("Legend", descr));

        JLabel title = new JLabel("Snake Game Controller");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(title);
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        namePanel.setBackground(PANEL_BACKGROUND);
        namePanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel nameLabel = new JLabel("Your name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        nameLabel.setForeground(TEXT_COLOR);

        playerNameField = new JTextField(15);
        playerNameField.setFont(new Font("Arial", Font.PLAIN, 14));
        playerNameField.setText(getDefaultPlayerName());
        playerNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));

        namePanel.add(nameLabel);
        namePanel.add(playerNameField);
        this.add(namePanel);
        this.add(Box.createRigidArea(new Dimension(0, 20)));
    }

    private String getDefaultPlayerName() {
        String osName = System.getProperty("user.name");
        if (osName != null && !osName.isEmpty()) {
            return osName;
        }
        return "Player";
    }

    public String getPlayerName() {
        String name = playerNameField.getText().trim();
        if (name.isEmpty()) {
            return "Player";
        }
        return name;
    }

    private JPanel createSection(String title, JPanel content) {
        JPanel section = new JPanel();
        section.setLayout(new BorderLayout());
        section.setBackground(PANEL_BACKGROUND);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));
        contentWrapper.setBackground(PANEL_BACKGROUND);
        contentWrapper.add(content, BorderLayout.CENTER);

        section.add(titleLabel, BorderLayout.NORTH);
        section.add(contentWrapper, BorderLayout.CENTER);

        return section;
    }

    private void initButtonsPanel() {
        buttons = new JPanel();
        buttons.setBackground(PANEL_BACKGROUND);
        buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton start = createStyledButton("New Game", Color.decode("#0A9165"));
        JButton quit = createStyledButton("Quit", Color.decode("#F44336"));

        start.addActionListener(actionEvent -> {
            String playerName = getPlayerName();
            masterController.startGame(playerName);
        });

        quit.addActionListener(actionEvent -> {
            masterController.stopGame();
        });

        buttons.add(start);
        buttons.add(quit);
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(baseColor.darker(), 2),
                new EmptyBorder(8, 20, 8, 20)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(baseColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(baseColor);
            }
        });

        return button;
    }

    private void initDescription() {
        this.descr = new JPanel();
        descr.setLayout(new BoxLayout(descr, BoxLayout.Y_AXIS));
        descr.setBackground(PANEL_BACKGROUND);

        addLegendItem("M", "Master (Host)");
        addLegendItem("D", "Deputy (Backup)");
        addLegendItem("-", "Normal Player");
        addLegendItem(" ", "Viewer (Spectator)");
    }

    private void addLegendItem(String icon, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
        item.setBackground(PANEL_BACKGROUND);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        textLabel.setForeground(TEXT_COLOR);

        item.add(iconLabel);
        item.add(textLabel);
        descr.add(item);
    }

    private JList<GameInfo> otherGames;

    private void initOtherBoardsPanel() {
        otherBoards = new JPanel();
        otherBoards.setLayout(new BorderLayout());
        otherBoards.setBackground(PANEL_BACKGROUND);

        otherGames = new JList<>();
        otherGames.setFixedCellWidth(400);
        otherGames.setFixedCellHeight(100);
        otherGames.setFont(new Font("Arial", Font.PLAIN, 12));
        otherGames.setDragEnabled(false);
        otherGames.addListSelectionListener(e -> {
            if(!otherGames.getValueIsAdjusting()) {
                masterController.connect(otherGames.getSelectedValue(), isViewer.isSelected());
            }
        });

        otherGames.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );

                if (value instanceof GameInfo) {
                    GameInfo game = (GameInfo) value;
                    String gameName = game.getGameSpecs().getGameName();
                    int playerCount = game.getGameSpecs().getPlayers().getPlayersCount();
                    int width = game.getGameSpecs().getConfig().getWidth();
                    int height = game.getGameSpecs().getConfig().getHeight();
                    int foodStatic = game.getGameSpecs().getConfig().getFoodStatic();
                    int delay = game.getGameSpecs().getConfig().getStateDelayMs();

                    String displayText = String.format(
                            "<html><b>%s</b><br>" +
                                    "Players: %d | Size: %dx%d<br>" +
                                    "Food: %d | Speed: %dms</html>",
                            truncate(gameName, 20),
                            playerCount,
                            width, height,
                            foodStatic,
                            delay
                    );

                    label.setText(displayText);
                }

                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 220), 1),
                        new EmptyBorder(8, 10, 8, 10)
                ));

                if (isSelected) {
                    label.setBackground(new Color(220, 230, 255));
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(index % 2 == 0 ?
                            new Color(245, 245, 255) :
                            new Color(235, 235, 245));
                    label.setForeground(new Color(60, 60, 80));
                }

                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(otherGames);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(420, 250));

        otherBoards.add(scrollPane, BorderLayout.CENTER);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    @Override
    public void updateGames(HashSet<GameInfo> games) {
        GameInfo[] gamesArr = games.toArray(new GameInfo[0]);
        otherGames.setListData(gamesArr);
    }
}