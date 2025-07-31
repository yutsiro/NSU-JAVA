package controller;

import model.GameModel;
import view.GameView;
import javax.swing.*;
import java.awt.event.*;

//связывает модель и представление, обрабатывает ввод
public class GameController {
    private GameModel model;//данные
    private GameView view;//интерфейс
    private Timer timer;//объект таймера, вызывает обновления каждые 20мс

    //конструктор
    public GameController(GameModel model, GameView view) {
        this.model = model;//текущий объект GameController . model
        this.view = view;
        //таймер срабатывает каждые 20мс
        timer = new Timer(20, e -> {
            model.update();
            view.repaintGame(model);
        });
    }
    //таймер начинает вызывать код из лямбда выражения в конструкторе каждые 20 мс
    public void startGame() {
        timer.start();
    }

    public void jump() {
        if (!model.isGameOver()) {
            model.jump();
        }
    }

    public void newGame() {
        timer.stop();
        model.reset();
        timer.start();
    }

    public void showHighScores() {
        view.showHighScores(model.getHighScores());
    }

    public void showAbout() {
        view.showAbout();
    }
}