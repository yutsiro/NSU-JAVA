package view;

import controller.GameController;
import model.GameModel;
import javax.swing.*;
import java.awt.event.*;
import java.util.List;

public class GameView extends JFrame {
    private GamePanel gamePanel;//экран/панель где рисуются все игровые эелементы
    private GameController controller;
    //конструктор настраивает окно, меню, игровую панель
    public GameView() {
        setTitle("Flappy Bird");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//крестик закрывает лол
        setResizable(false);//нельзя растягивать окно

        //создание меню
        JMenuBar menuBar = new JMenuBar();//полоса вверху окна - меню
        JMenu gameMenu = new JMenu("Game");//ниже настраиваем пункты в выпадающем списке меню
        JMenuItem newGame = new JMenuItem("New Game");
        JMenuItem highScores = new JMenuItem("High Scores");
        JMenuItem about = new JMenuItem("About");
        JMenuItem exit = new JMenuItem("Exit");

        newGame.addActionListener(e -> controller.newGame());
        highScores.addActionListener(e -> controller.showHighScores());
        about.addActionListener(e -> controller.showAbout());
        exit.addActionListener(e -> System.exit(0));

        gameMenu.add(newGame);
        gameMenu.add(highScores);
        gameMenu.add(about);
        gameMenu.add(exit);
        menuBar.add(gameMenu);//добавляем меню "Game" в панельку
        setJMenuBar(menuBar);

        //панель игры
        gamePanel = new GamePanel();
        add(gamePanel);//добавляет панель в окно
        setVisible(true);//делает окно видимым (иначе будет создано, но не отображаться)
    }
    //связываем интерфейс с контроллером, настройка обработки клавиш
    public void setController(GameController controller) {
        this.controller = controller;
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    controller.jump();
                }
            }
        });
        gamePanel.setFocusable(true);//делает gamePanel способной реагировать на клавиши
        gamePanel.requestFocusInWindow();//gamePanel принимает клавиши сразу как окно открывается
    }

    public void repaintGame(GameModel model) {
        gamePanel.setModel(model);//загружаем данные, которые нужно отрисовать
        gamePanel.repaint();//рисуем
    }

    public void showHighScores(List<Integer> scores) {
        StringBuilder sb = new StringBuilder("High Scores:\n");
        for (int i = 0; i < scores.size(); i++) {
            sb.append(i + 1).append(". ").append(scores.get(i)).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "High Scores", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showAbout() {
        JOptionPane.showMessageDialog(this, "Flappy Bird\nUse SPACE key to play.\n2025", "About", JOptionPane.INFORMATION_MESSAGE);
    }
}