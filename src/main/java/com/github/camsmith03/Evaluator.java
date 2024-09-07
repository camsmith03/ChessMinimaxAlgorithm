package com.github.camsmith03;

/**
 * Evaluator has the heuristics used to take a boardController in its static state, apply
 * an integer to represent its overall value, then return it to be utilized by
 * minimax. Changes to the heuristic have massive effects on the minimax output.
 * TODO: this is the weakest link for all the methods created. fixing the issues
 *       and improving the heuristic should solve most (if not all) of the
 *       issues for minimax.
 *
 * @author Cameron Smith
 * @version 09.04.2024
 */
public class Evaluator {
    private boolean awardCentral = true; // flip to false after 10+ moves are made (15+ post move boardController gets evaluated)
    private int evalMultiplier = 1;
    private final Bitboard board;
    private static final int[] PIECE_VAL = new int[]{1, 3, 3, 5, 9}; // official piece values.
    private static final long centralSquares = 0x0000001818000000L; // center four squares
    private static final long outerRing = 0x00003C24243C0000L; // ring surrounding center 4 squares


    /**
     * Constructor for Evaluator.
     *
     * @param board
     *      bitboard to be modified at runtime by Minimax.
     * @param maximizer
     *      color for the lifetime of the program.
     * @param treeDepth
     *      TODO: unused
     */
    public Evaluator(Bitboard board, Piece.Color maximizer, int treeDepth) {
        if (maximizer == Piece.Color.BLACK) {
            evalMultiplier = -1; // evaluate(Move) defaults to assume black is the minimizer.
                                 // multiplying by negative 1 will reverse the output.
        }


        this.board = board;
    }

    // assumes that a try/catch was already made on the move to ensure its legality, and that the move has been applied
    // to the virtual boards.
    // Will also base calculations such that white -> maximizer, black -> minimizer. A multiplier initialized in the
    // constructor will invert the output assuming the player is actually black.

    /**
     * Assuming a try/catch was made to ensure the move is considered legal, and
     * that the move was applied to the virtual boards, this will return an
     * integer value corresponding the static evaluation of the boardController. The
     * helper methods base their calculations on white being the maximizer,
     * which the evalMultiplier uses to invert their outputs if that isn't the
     * case.
     *
     * @param move
     *      already applied the current boardController. TODO: no longer used
     * @return integer representing the static evaluation.
     */
    public int evaluate(Move move) {
        long[][] boards = board.getVirtualBoards();
        long[] colorBoards = board.getVirtualColorBoards();

        int evaluation = materialEval(boards) + developmentEval(boards, colorBoards);

        return evaluation * evalMultiplier; // TODO: evalMultiplier can't tell treeDepth for differentiation!!
    }



    /**
     * Evaluates who has the material advantage for the current boardController. Returns
     * an integer assuming white is the maximizer.
     *
     * @param boards
     *      representing the current boardController configuration.
     * @return evaluation under the material calculations.
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

    /**
     * Evaluates who has better developed pieces for the current boardController. The
     * flag "awardCentral" is controllable from the outside to stop providing
     * incentives towards the central squares after a certain number of moves
     * have passed.
     *
     * @param boards
     *      representing the current boardController configuration.
     * @param colorBoards
     *      for the piece designated values.
     * @return evaluation under the development calculations.
     */
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

    /**
     * After a certain number of moves have been made, the number of moves that
     * will be applied to the boardController will mean the mid-game is approaching its
     * conclusion and end game prep should be made. Awarding central square
     * bonuses is trivial since the stability of the central squares is not as
     * big a concern.
     * <br>
     * This is left to be utilized by Minimax to allow multiple configurations
     * to be attempted and see which one leads to a preferable outcome.
     */
    public void stopCentralBonus() {
        awardCentral = false;
    }

}
