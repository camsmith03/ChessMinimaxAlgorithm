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

    public void move(Move movedPiece) {
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

    public MoveList getLegalMoves() {
        MoveList possibleMoves = moveGenerator.generateMoves(gameBoard, turnToMove);


        return null;
    }

    /**
     * Things to filter:
     *      -> Pawn en passant
     *      -> King in check
     *          -> Illegally putting king in check
     *      -> Castling behavior
     *      -> Pawn promotion
     *
     * NOTE: This is handled by the evaluator!
     *          -> Illegal moves when king currently in check
     *
     * @param possibleMoves
     */
    private void filterMoves(ArrayList<Move> possibleMoves) {

    }
}
