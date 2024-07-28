package com.github.camsmith03;

/**
 * Square serves as a positional object that adheres to the boundaries of the chess board. The x and y coordinates are
 * used to designate the location for a piece. In this instance x = file - 1, y = rank - 1
 *
 */
public class Square {
    private final int x;
    private final int y;

    public Square(int x, int y ) {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            throw new IllegalArgumentException("Coordinates must be between 0 and 7");
        }
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
