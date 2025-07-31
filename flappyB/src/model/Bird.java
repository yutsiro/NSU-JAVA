package model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Bird extends GameObject {
    private int velocity;

    public Bird() {
        super(100, 300); //x=100 фиксировано, y=300
        velocity = 0;
    }

    @Override
    public void update() {
        velocity += 1;
        y += velocity;
    }

    public void jump() {
        velocity = -15;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 30, 30);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.PINK);
        Rectangle bounds = getBounds();
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    public boolean isOffScreen() {
        return false; //птица никогда не выходит за экран
    }
}