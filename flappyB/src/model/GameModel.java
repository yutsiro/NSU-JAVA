package model;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameModel {
    public enum GameState { MENU, PLAYING, GAME_OVER }

    private Bird bird;
    private List<GameObject> gameObjects;
    private int score;
    private boolean gameOver;
    private List<Integer> highScores;
    private GameState gameState;
    private static final String HIGH_SCORES_FILE = "highscores.txt";

    public GameModel() {
        gameObjects = new ArrayList<>();
        gameObjects.add(new Cloud(200, 100));
        gameObjects.add(new Cloud(400, 150));
        bird = new Bird();
        gameObjects.add(bird);
        gameObjects.add(new Pipe(600));
        score = 0;
        gameOver = false;
        gameState = GameState.MENU;
        highScores = new ArrayList<>();
        loadHighScores();
    }

    public Bird getBird() { return bird; }
    public List<Pipe> getPipes() {
        List<Pipe> pipes = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            if (obj instanceof Pipe) {
                pipes.add((Pipe) obj);
            }
        }
        return Collections.unmodifiableList(pipes);
    }
    public List<Cloud> getClouds() {
        List<Cloud> clouds = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            if (obj instanceof Cloud) {
                clouds.add((Cloud) obj);
            }
        }
        return Collections.unmodifiableList(clouds);
    }
    public List<Drawable> getDrawables() {
        return new ArrayList<>(gameObjects); //все GameObject реализуют интфс Drawable
    }
    public int getScore() { return score; }
    public boolean isGameOver() { return gameOver; }
    public List<Integer> getHighScores() { return highScores; }
    public GameState getGameState() { return gameState; }

    public void update() {
        if (!gameOver && gameState == GameState.PLAYING) {
            for (GameObject obj : gameObjects) {
                obj.update();
            }
            gameObjects.removeIf(GameObject::isOffScreen);
            for (GameObject obj : gameObjects) {
                if (obj instanceof Pipe pipe && pipe.getX() < 100 && pipe.getX() > 90) {
                    score++;
                }
            }
            Pipe lastPipe = getLastPipe();
            if (lastPipe != null && lastPipe.getX() < 400) {
                Pipe newPipe = new Pipe(600);
                gameObjects.add(newPipe);
            }
            checkCollisions();
        }
    }

    public void jump() {
        if (gameState == GameState.PLAYING) {
            bird.jump();
        }
    }

    public void startGame() {
        if (gameState == GameState.MENU || gameState == GameState.GAME_OVER) {
            reset();
            gameState = GameState.PLAYING;
            gameOver = false;
        }
    }

    public void exit() {
        System.exit(0);
    }

    private void checkCollisions() {
        Rectangle birdRect = bird.getBounds();
        for (GameObject obj : gameObjects) {
            if (obj instanceof Pipe pipe) {
                Rectangle topPipe = pipe.getTopBounds();
                Rectangle bottomPipe = pipe.getBottomBounds();
                if (birdRect.intersects(topPipe) || birdRect.intersects(bottomPipe) || bird.getY() > 550 || bird.getY() < 0) {
                    gameOver = true;
                    gameState = GameState.GAME_OVER;
                    highScores.add(score);
                    Collections.sort(highScores, Collections.reverseOrder());
                    if (highScores.size() > 5) {
                        highScores = highScores.subList(0, 5);
                    }
                    saveHighScores();
                }
            }
        }
    }

    public void reset() {
        gameObjects.clear();
        gameObjects.add(new Cloud(200, 100));
        gameObjects.add(new Cloud(400, 150));
        bird = new Bird();
        gameObjects.add(bird);
        gameObjects.add(new Pipe(600));
        score = 0;
        gameOver = false;
        gameState = GameState.MENU;
    }

    private Pipe getLastPipe() {
        Pipe lastPipe = null;
        for (GameObject obj : gameObjects) {
            if (obj instanceof Pipe) {
                lastPipe = (Pipe) obj;
            }
        }
        return lastPipe;
    }

    private void loadHighScores() {
        try (BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    highScores.add(Integer.parseInt(line));
                } catch (NumberFormatException ignored) {}
            }
            Collections.sort(highScores, Collections.reverseOrder());
            if (highScores.size() > 5) highScores = highScores.subList(0, 5);
        } catch (FileNotFoundException e) {
            highScores = new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла рекордов: " + e.getMessage());
        }
    }

    private void saveHighScores() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HIGH_SCORES_FILE))) {
            for (int score : highScores) {
                writer.write(String.valueOf(score));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при записи файла рекордов: " + e.getMessage());
        }
    }
}