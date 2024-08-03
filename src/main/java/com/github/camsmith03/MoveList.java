package com.github.camsmith03;
import java.lang.reflect.Array;

public class MoveList {
    private final LinkedList<Move>[] moves;
    private static final int LIST_SIZE = 12;
    private final FreeMoves freeList = new FreeMoves();

    @SuppressWarnings("unchecked")
    public MoveList() {
        moves = (LinkedList<Move>[]) Array.newInstance(LinkedList.class, 12);
        for (int i = 0; i < LIST_SIZE; i++) {
            moves[i] = new LinkedList<>();
        }
    }

    public LinkedList<Move> getList(Piece.Color color, Piece.Type type) {
        return moves[getLLIndex(color, type)];
    }

    public void add(long fromMask, long toMask, Piece.Type pieceType, Piece.Color pieceColor, Piece.Type capturedType, Piece.Type promotedType) {
        int index = getLLIndex(pieceColor, pieceType);
        LinkedList.Node<Move> move = freeList.getMove(fromMask, toMask, pieceType, pieceColor, capturedType, promotedType);
        move.next = moves[index].head;
        moves[index].head = move;
    }

    public void deleteMove(Piece.Color color, Piece.Type type, Move move) {
        int index = getLLIndex(color, type);
        freeList.freeNode(moves[index].delete(move));
    }

    public void addMove(Move move) {
        int index = getLLIndex(move.getMovedPieceColor(), move.getMovedPieceType());
        LinkedList.Node<Move> addedNode = new LinkedList.Node<>();
        addedNode.data = move;
        addedNode.next = moves[index].head;
        moves[index].head = addedNode;
    }

    public void clearList() {
        for (int i = 0; i < LIST_SIZE; i++) {
            moves[i].head = null; // TODO: instead of nulling it out, garbage collect unused move nodes
        }
    }


    private int getLLIndex(Piece.Color color, Piece.Type type) {
        if (type == Piece.Type.NONE)
            throw new IllegalArgumentException("Type cannot be NONE");

        int index = 0;

        if (color == Piece.Color.BLACK) {
            index = 6;
        }

        return index + type.ordinal();
    }


    private static class FreeMoves {
        private final LinkedList<Move> freeMoves;
        private int size;

        public FreeMoves() {
            freeMoves = new LinkedList<>();
            size = 0;
        }

        public void freeNode(LinkedList.Node<Move> moveNode) {
            moveNode.next = freeMoves.head;
            freeMoves.head = moveNode;
            size += 1;
        }

        public LinkedList.Node<Move> getMove(long from, long to, Piece.Type pieceType, Piece.Color pieceColor, Piece.Type capturedPiece, Piece.Type promotedPiece) {
            if (size == 0) {
                LinkedList.Node<Move> moveNode = new LinkedList.Node<>();
                if (promotedPiece != Piece.Type.NONE) {
                    moveNode.data = new Move(from, to, pieceType, pieceColor, capturedPiece, promotedPiece);
                }
                else {
                    moveNode.data = new Move(from, to, pieceType, pieceColor, capturedPiece);
                }
                return moveNode;
            }
            size -= 1;

            LinkedList.Node<Move> rebuilt = freeMoves.head;
            freeMoves.head = rebuilt.next;
            rebuilt.next = null;
            rebuilt.data.rebuild(from, to, pieceType, pieceColor, capturedPiece, promotedPiece);

            return rebuilt;
        }



    }
}
