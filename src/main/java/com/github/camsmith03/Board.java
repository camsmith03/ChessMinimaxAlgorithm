package com.github.camsmith03;
import java.util.ArrayList;

/**
 * Used to represent the board for the current game state. Interacts directly with Bitboard, and helps to ensure move
 * legality is maintained.
 */
public class Board {
    private Bitboard gameBoard;
    private Piece.Color turnToMove;
    private MoveGenerator moveGenerator;

    public Board() {
        gameBoard = new Bitboard();
        moveGenerator = new MoveGenerator();
        turnToMove = Piece.Color.WHITE;
    }

    public Bitboard getBitboard() {
        return gameBoard;
    }


    public void cleanBoard() {
        gameBoard = new Bitboard();
        moveGenerator = new MoveGenerator();
        turnToMove = Piece.Color.WHITE;
    }

    public void move(Move movedPiece) throws IllegalArgumentException {
        gameBoard.movePiece(movedPiece);
        changeTurn();
    }

    private void changeTurn() {
        if (turnToMove == Piece.Color.WHITE) {
            turnToMove = Piece.Color.BLACK;
        }
        else {
            turnToMove = Piece.Color.WHITE;
        }
    }

    public Piece.Color getTurn() {
        return turnToMove;
    }

    public MoveList getLegalMoves() {
        return moveGenerator.generateMoves(gameBoard, turnToMove);
    }

    public Piece getPiece(long position) {
        return gameBoard.getPiece(position);
    }
}
