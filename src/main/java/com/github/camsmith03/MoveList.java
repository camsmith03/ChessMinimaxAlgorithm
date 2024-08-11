package com.github.camsmith03;
import java.lang.reflect.Array;

// Changes: Removed the unnecessary separation of the type linked lists. Condensed into one linked list, as it made more
//          sense under the circumstances.
public class MoveList {
//    private final LinkedList<Move>[] moves;
    private LinkedList<Move> bucket01; // first priority bucket
    private LinkedList<Move> bucket02; // second priority bucket
    private LinkedList<Move> remainingMoves; // last moves in the priority queue
    private final LinkedList.Node<Move> bucket01Tail = new LinkedList.Node<>();
    private final LinkedList.Node<Move> bucket02Tail = new LinkedList.Node<>();
    private static final int LIST_SIZE = 12;
    private final FreeMoves freeList = new FreeMoves();

//    @SuppressWarnings("unchecked")
    public MoveList() {
        remainingMoves = new LinkedList<>();
        bucket01 = new LinkedList<>();
        bucket02 = new LinkedList<>();

        // global bucket tail pointers (not stored in the buckets themselves)
        bucket01Tail.next = bucket01.head;
        bucket02Tail.next = bucket02.head;
        // Lists are separately partitioned until iteration is required
        // then it will follow: bucket01 -> bucket02 -> remainingMoves
    }

    public void add(long fromMask, long toMask, Piece.Type pieceType, Piece.Color pieceColor, Piece.Type capturedType, Piece.Type promotedType) {
        LinkedList.Node<Move> move = freeList.getMove(fromMask, toMask, pieceType, pieceColor, capturedType, promotedType);
        if (capturedType != Piece.Type.NONE) {
            // add to priority bucket 02
            move.next = bucket02.head;
            bucket02.head = move;
        }
        else {
            move.next = remainingMoves.head;
            remainingMoves.head = move;
        }

    }

    public LinkedList.Node<Move> getMoveListHead() {
        LinkedList.Node<Move> headNode;

        if (bucket02.head.data != null) {
            bucket02Tail.next = remainingMoves.head;
            if (bucket01.head.data != null) {
                bucket01Tail.next = bucket01.head;
                headNode = bucket01.head;
            }
            else {
                headNode = bucket02.head;
            }
        }
        else if (bucket01.head.data != null) {
            headNode = bucket01.head;
            bucket01Tail.next = remainingMoves.head;
        }
        else {
            headNode = remainingMoves.head;
        }

        return headNode;
    }

    public void addMove(Move move) {
        LinkedList.Node<Move> addedNode = new LinkedList.Node<>();
        if (move.getEnPassant() != 0) {
            // Move was an en passant capture. Add it to bucket02

        }
//        addedNode.data = move;
//        addedNode.next = moves.head;
//        moves.head = addedNode;
    }

    public void clearList() {
        remainingMoves = new LinkedList<>();
        bucket01 = new LinkedList<>();
        bucket02 = new LinkedList<>();
        bucket01Tail.next = bucket01.head;
        bucket02Tail.next = bucket02.head;
    }


//    private int getLLIndex(Piece.Color color, Piece.Type type) {
//        if (type == Piece.Type.NONE)
//            throw new IllegalArgumentException("Type cannot be NONE");
//
//        int index = 0;
//
//        if (color == Piece.Color.BLACK) {
//            index = 6;
//        }
//
//        return index + type.ordinal();
//    }


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
