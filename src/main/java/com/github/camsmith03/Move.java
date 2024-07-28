package com.github.camsmith03;

/**
 * Will be used to determine all possible moves from a given Board.
 *
 */
public class Move {
    private Square from;
    private Square to;
    private GamePiece.PieceType capturedPiece;

    public Move(Square from, Square to) {
        this.from = from;
        this.to = to;
        capturedPiece = GamePiece.PieceType.NONE;

    }

    public Move(Square from, Square to, GamePiece.PieceType capturedPiece) {
        this.from = from;
        this.to = to;
        this.capturedPiece = capturedPiece;
    }

    public Square getFrom() {
        return from;
    }

    public Square getTo() {
        return to;
    }
}
