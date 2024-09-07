package com.github.camsmith03;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Fairly simplistic yet straightforward Priority Queue for moves. This class
 * serves to store the output from the MoveGenerator, and will automatically
 * order the moves based on the comparator from the Move compareTo
 * implementation.
 *
 * @author Cameron Smith
 * @version 08.26.2024
 */
public class MoveList {
    private final PriorityQueue<Move> moveList;
    private static final int LIST_SIZE = 30;

    /**
     * Initializes the Priority Queue with the expected comparator.
     */
    public MoveList() {
        Comparator<Move> comparator = Move::compareTo;
        moveList = new PriorityQueue<>(LIST_SIZE, comparator);
    }

    /**
     * Creates the move from the supplied arguments and passes said move to the
     * PriorityQueue. Any errors that arise from the parameters will be thrown
     * from Move's constructor.
     *
     * @param fromMask
     *      masking bit representing the piece's initial location.
     * @param toMask
     *      masking bit representing the piece's final location.
     * @param pieceType
     *      type classifier for the piece that was moved.
     * @param pieceColor
     *      color classifier for the piece that was moved
     * @param capturedType
     *      type classifier for the piece that was captured (or NONE)
     * @param promotedType
     *      type classifier for the type the piece promoted to (or NONE)
     */
    public void add(long fromMask, long toMask, Piece.Type pieceType, Piece.Color pieceColor, Piece.Type capturedType,
                    Piece.Type promotedType) {
        moveList.add(new Move(fromMask, toMask, pieceType, pieceColor, capturedType, promotedType));
    }

    /**
     * Obtains and returns the first element in the PriorityQueue. This element
     * is subsequently removed from the PriorityQueue itself.
     *
     * @return Move
     */
    public Move pop() {
        return moveList.poll();
    }

    /**
     * Simple way to add a move to the MoveList without providing the raw
     * arguments, instead opting to provide the object itself. This is useful
     * for some special case moves (like castling or en passant) that require
     * field manipulation to accurately be interpreted from the algorithm. Any
     * move that doesn't fit this category shouldn't be calling this method.
     *
     * @param move
     *      Move to apply to the PriorityQueue
     */
    public void addMove(Move move) {
        moveList.add(move);
    }

    /**
     * Removes all the elements of the list. Achieves the same result as
     * creating a new object, without the GC overhead.
     * TODO: implement this into the minimax algorithm.
     */
    public void clearList() {
        moveList.clear();
    }

    /**
     * Method to check if the list is empty.
     *
     * @return true if empty; false otherwise.
     */
    public boolean isEmpty() { return moveList.isEmpty(); }

    /**
     * Getter for the number of elements currently in the moveList.
     *
     * @return number of elements
     */
    public int size() { return moveList.size(); }
}