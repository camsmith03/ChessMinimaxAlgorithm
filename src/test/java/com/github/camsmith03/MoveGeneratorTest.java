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
        ArrayList<Move> moves = moveGenerator.generateMoves(board);
        System.out.println(moves);
    }
}