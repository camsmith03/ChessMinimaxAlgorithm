package com.github.camsmith03;

/**
 * Used to represent the board for the current game state. Interacts directly with Bitboard, and helps to ensure move
 * legality is maintained.
 */
public class Board {
    private Bitboard gameBoard;
    private boolean whiteTurn;

    public Board() {
        gameBoard = new Bitboard();
        whiteTurn = true;
    }

    public Bitboard getBitboard() {
        return gameBoard;
    }

    public GamePiece.PieceColor getTurn() {
        if (whiteTurn) {
            return GamePiece.PieceColor.WHITE;
        }
        return GamePiece.PieceColor.BLACK;
    }
}
