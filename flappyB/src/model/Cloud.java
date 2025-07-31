package model;

import java.awt.Color;
import java.awt.Graphics;

public class Cloud extends GameObject {
    private int width, height;
    private int speed;

    public Cloud(int x, int y) {
        super(x, y);
        this.width = 60;
        this.height = 40;
        this.speed = 2;
    }

    @Override
    public void update() {
        x -= speed;
        if (x + width < 0) {
            x = 600;
            y = 50 + (int)(Math.random() * 150);
        }
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillOval(x, y, width, height);
    }

    @Override
    public boolean isOffScreen() {
        return x + width < 0; //облако выходит когда фулл за экраном
    }
}