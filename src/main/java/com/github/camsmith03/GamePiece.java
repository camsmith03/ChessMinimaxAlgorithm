package com.github.camsmith03;

public class GamePiece {
    public enum PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING, NONE }
    public enum PieceColor { WHITE, BLACK }

    private PieceType type;
    private PieceColor color;

    public GamePiece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
    }

    public PieceType getType() {
        return type;
    }

    public PieceColor getColor() {
        return color;
    }
}
