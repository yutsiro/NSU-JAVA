package model;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Pipe extends GameObject {
    private int gapY;

    public Pipe(int x) {
        super(x, 0); //y=0, так как трубы от верха экрана
        this.gapY = 200 + (int)(Math.random() * 200); //(от 200 до 399)
    }

    public int getGapY() { return gapY; }

    @Override
    public void update() {
        x -= 5;//сдвиг влево
    }

    public Rectangle getTopBounds() {
        return new Rectangle(x, 0, 50, gapY - 100);
    }

    public Rectangle getBottomBounds() {
        return new Rectangle(x, gapY + 100, 50, 600 - (gapY + 100));
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(x, 0, 50, gapY - 100);
        g.fillRect(x, gapY + 100, 50, 600 - (gapY + 100));
    }
}