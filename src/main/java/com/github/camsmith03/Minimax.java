package com.github.camsmith03;

public class Minimax {
    private Evaluator evaluator;
    private Board board;
    private static final int STARTING_DEPTH = 10;

    public Minimax(Piece.Color startingColor) {
        board = new Board();
        evaluator = new Evaluator();
    }

//    public Move minimax(Board board, int depth) {
//
//        return null;
//    }
}
