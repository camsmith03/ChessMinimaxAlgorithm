package com.github.camsmith03;
import java.util.ArrayList;

/**
 * Used to represent the board for the current game state. Interacts directly with Bitboard, and helps to ensure move
 * legality is maintained.
 */
public class Board {
    private Bitboard board;
    private Piece.Color turnToMove;
    private MoveGenerator moveGenerator;

    public Board() {
        board = new Bitboard();
        moveGenerator = new MoveGenerator();
        turnToMove = Piece.Color.WHITE;
    }

    public Bitboard getBitboard() {
        return board;
    }


    public void cleanBoard() {
        board = new Bitboard();
        moveGenerator = new MoveGenerator();
        turnToMove = Piece.Color.WHITE;
    }

    public void move(Move movedPiece) throws IllegalArgumentException {
        board.movePiece(movedPiece);
        changeTurn();
    }

    public void applyVirtualMove() {
        board.applyVirtualMovePermanently();
        changeTurn();
    }

    public SaveState saveGameState() {
        return board.saveCurrentState();
    }

    public void restoreGameState(SaveState saveState, Piece.Color turnToMove) {
        this.turnToMove = turnToMove;
        board.restoreSaveState(saveState);
    }

    public void applyMinimaxMove(Move tempMove) throws IllegalArgumentException, RuntimeException {
        try {
            board.virtualMovePiece(tempMove);
            if (board.kingMissing()) {
                throw new RuntimeException(); // game is over (one of the kings is no longer there)
            }
        } catch (IllegalArgumentException e) {
            board.wipeVirtualization();
            throw e;
        }
        board.applyVirtualMovePermanently();

        changeTurn();
    }

    protected void changeTurn() {
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
        return moveGenerator.generateMoves(board, turnToMove);
    }

    public Piece getPiece(long position) {
        return board.getPiece(position);
    }
}
