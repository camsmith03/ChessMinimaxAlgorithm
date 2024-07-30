package com.github.camsmith03;

public class Piece {
    public enum Type { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING, NONE }
    public enum Color { WHITE, BLACK }

    public final Type type;
    public final Color color;

    public Piece(Type type, Color color) {
        this.type = type;
        this.color = color;
    }
}
