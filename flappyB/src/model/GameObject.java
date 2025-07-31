package model;

import java.awt.Graphics;

public abstract class GameObject implements Drawable {
    protected int x, y;

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public abstract void update();

    public boolean isOffScreen() {
        return x < -50;//общий критерий выхода за экран
    }
}