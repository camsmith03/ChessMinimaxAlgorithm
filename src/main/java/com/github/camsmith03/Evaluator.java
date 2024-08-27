package com.github.camsmith03;

public class Evaluator {

    /*
    GOALS:
        - evaluateMoves(MoveList, boolean buildMax) -> Heap
        - evaluate(Bitboard) -> int

    PLAN:

        We need to organize the structure of this class to prevent confusion.

        First, given a bitboard, this must be able to compute an int value corresponding with some predefined weights.

        Considerations for different weights:
            - Piece value
            - Control over center
            - Moves that put enemy king in check
            - Piece captures
            - Castling weight
            etc...

        These weights should be adjustable so that small tweaks can be made and tested to find the perfect configuration
        of them all.


        Second, we have to determine who will be building the heap of evaluated integers.

            It could be...
                - Evaluator -> this is a valid approach, but it also introduces the need to interface between MoveList.

                - MoveList -> this would allow MoveList to build a heap, although, would it take away from MoveList
                              being solely a data structure

                - Minimax  -> don't really know if its the best or worst approach as of right now.


            We know our input will be a MoveList. This puts MoveList as a valid contender for offering the best
            solution. We can create a Heap data structure designed for KVPairs of Moves and Integers, then interface
            with Evaluator for every MoveList element until they are all appended to the Heap.

            Note, we designed MoveList to be an array of LinkedLists, which has the benefit of separating the piece
            moves by type. Knowing this, moves that tend to lead to higher or lower values based on type can be computed
            before others to speed up Heap construction. This efficiency boost would probably be minimal at best, which
            puts our design for MoveList at a disadvantage.

            MoveList does have the ability to quickly clear a list. This means we really only need one object to
            represent it.

            Think about how this would work:
                - including a tempMove() method to bitboard (update boards but keep tempBoards as copy),
                - passing bitboard to evaluator,
                - evaluating boards,
                - undoing said move with an undoMove() method (restoring copy).

            Is this the best approach? Is there a better alternative? What about Board?

            Should Evaluator be passed a Move instead?
                No, we want the evaluation to be done on static boards.

            This is a tough one

     */
    private final Piece.Color maximizer;
    private final Piece.Color minimizer;
    private final Piece.Color finalMoveColor;
    private final boolean playerMove;
    private boolean awardCentral = true; // flip to false after 10+ moves are made (15+ post move board gets evaluated)
    private int evalMultiplier = 1;
    private final Bitboard board;
    private static final int[] PIECE_VAL = new int[]{1, 3, 3, 5, 9}; // official piece values.
    private static final long centralSquares = 0x0000001818000000L; // center four squares
    private static final long outerRing = 0x00003C24243C0000L; // ring surrounding center 4 squares


    public Evaluator(Bitboard board, Piece.Color maximizer, int treeDepth) {

        this.maximizer = maximizer;
        if (maximizer == Piece.Color.BLACK) {
            this.minimizer = Piece.Color.WHITE;
            evalMultiplier = -1; // evaluate(Move) defaults to assume black is the minimizer.
                                 // multiplying by negative 1 will reverse the output.
        }
        else
            this.minimizer = Piece.Color.BLACK;

        this.board = board;
        if (treeDepth % 2 == 0) {
            finalMoveColor = minimizer;
            playerMove = finalMoveColor == Piece.Color.WHITE;
        }
        else {
            finalMoveColor = maximizer;
            playerMove = finalMoveColor == Piece.Color.BLACK;
        }

    }

    // assumes that a try/catch was already made on the move to ensure its legality, and that the move has been applied
    // to the virtual boards.
    // Will also base calculations such that white -> maximizer, black -> minimizer. A multiplier initialized in the
    // constructor will invert the output assuming the player is actually black.
    public int evaluate(Move move) {
        long[][] boards = board.getVirtualBoards();
        long[] colorBoards = board.getVirtualColorBoards();

        int evaluation = materialEval(boards) + developmentEval(boards, colorBoards) + safetyEval(boards);

//        if (move.getPromotedType() != Piece.Type.NONE) {
//            // TODO: Come back and rethink promotion rewards. Promotion may be an intermittent move that was made. This
//            //       goes against static definition of static evaluation.
//            if (playerMove)
//                evaluation += 50; // encourage player promotion
//            else
//                evaluation -= 50; // discourage opponent promotion
//        }
//        else if (move.getCastledRook() != Move.CastleSide.NONE) {
//            if (playerMove) {
//                evaluation += 50;
//            }
//        }

        return evaluation * evalMultiplier;
    }



    /**
     * Evaluates who has the material advantage for the current board. Returns an integer assuming that black is the
     * minimizer (can multiply by negative 1 to invert it if not).
     * <br>
     * Considerations:
     *  -> High value of Queen
     *  -> Pair of two bishops on the board
     *  -> General piece value
     *
     * @return
     */
    private int materialEval(long[][] boards) {
        int blackMaterial = 0;
        int whiteMaterial = 0;

        // Baseline values
        for (int i = 0; i < 5; i++) {
            long whitePieces = boards[0][i];
            while (whitePieces != 0) {
                whiteMaterial += PIECE_VAL[i];
                whitePieces = whitePieces ^ (whitePieces & -whitePieces);
            }
            long blackPieces = boards[1][i];
            while (blackPieces != 0) {
                blackMaterial += PIECE_VAL[i];
                blackPieces = blackPieces ^ (blackPieces & -blackPieces);
            }
        }

        // Bishop pairs (bonus points)
        long whiteBishops = boards[0][2];
        if ((whiteBishops ^ (whiteBishops & -whiteBishops)) != 0) {
            whiteMaterial += 2;
        }
        long blackBishops = boards[1][2];
        if ((blackBishops ^ (blackBishops & -blackBishops)) != 0) {
            blackMaterial += 2;
        }

        return whiteMaterial - blackMaterial; // positive result means white advantage, negative result means black
                                              // advantage.
    }

    private int developmentEval(long[][] boards, long[] colorBoards) {
        // Pawn structure, Piece placement, Open Files and Diagonals, Piece mobility
        int whiteDevelopment = 0;
        int blackDevelopment = 0;

        if (awardCentral) {
            long whitePiecesInner = colorBoards[0] & centralSquares;
            long blackPiecesInner = colorBoards[1] & centralSquares;

            while (whitePiecesInner != 0) {
                whiteDevelopment += 2;
                whitePiecesInner = whitePiecesInner ^ (whitePiecesInner & -whitePiecesInner);
            }
            while (blackPiecesInner != 0) {
                blackDevelopment += 2;
                blackPiecesInner = blackPiecesInner ^ (blackPiecesInner & -blackPiecesInner);
            }

            long whitePiecesOuter = colorBoards[0] & outerRing;
            long blackPiecesOuter = colorBoards[1] & outerRing;

            while (whitePiecesOuter != 0) {
                whiteDevelopment += 1;
                whitePiecesOuter = whitePiecesOuter ^ (whitePiecesOuter & -whitePiecesOuter);
            }

            while (blackPiecesOuter != 0) {
                blackDevelopment += 1;
                blackPiecesOuter = blackPiecesOuter ^ (blackPiecesOuter & -blackPiecesOuter);
            }
        }
        return whiteDevelopment - blackDevelopment;
    }

    private int safetyEval(long[][] boards) {
        return 0;
    }

    /**
     * After 20+ moves have been made, the number of moves that will be applied to the board will mean the mid-game is
     * approaching its conclusion and end game prep should be made. Awarding central square bonuses is trivial since the
     * stability of the central squares is not as big a concern.
     * <br>
     * This is left to be utilized by Minimax to allow multiple configurations to be attempted and see which one leads
     * to a preferable outcome.
     *
     */
    public void stopCentralBonus() {
        awardCentral = false;
    }

}
