package com.github.camsmith03;
import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.PriorityQueue;

public class MoveList {
    private final PriorityQueue<Move> moveList;
    private static final int LIST_SIZE = 30;

    public MoveList() {
        Comparator<Move> comparator = Move::compareTo;
        moveList = new PriorityQueue<Move>(LIST_SIZE, comparator);
    }

    public void add(long fromMask, long toMask, Piece.Type pieceType, Piece.Color pieceColor, Piece.Type capturedType, Piece.Type promotedType) {
        moveList.add(new Move(fromMask, toMask, pieceType, pieceColor, capturedType, promotedType));
    }


    public Move pop() {
        return moveList.poll();
    }

    public void addMove(Move move) {
        moveList.add(move);
    }

    public void clearList() {
        moveList.clear();
    }

    public boolean isEmpty() { return moveList.isEmpty(); }

    public int size() { return moveList.size(); }
}
