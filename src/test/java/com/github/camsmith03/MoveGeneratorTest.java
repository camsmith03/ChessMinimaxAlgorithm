package com.github.camsmith03;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class MoveGeneratorTest {
    private MoveGenerator moveGenerator;
    private Board board;

    @BeforeEach
    void setUp() {
        moveGenerator = new MoveGenerator();
        board = new Board();
    }

    @Test
    void testMoveGenerator() {
        ArrayList<Move> moves; //  = moveGenerator.generateMoves(board);
//        System.out.println("done");

        // White: d4
        // Black: g5
        board.getBitboard().movePiece(new GamePiece(GamePiece.PieceType.PAWN, GamePiece.PieceColor.WHITE), new Square(3, 1), new Square(3, 3));
        board.getBitboard().movePiece(new GamePiece(GamePiece.PieceType.PAWN, GamePiece.PieceColor.BLACK), new Square(6, 6), new Square(6, 4));

        moves = moveGenerator.generateMoves(board);
//        System.out.println("done");
    }
}