package com.github.camsmith03;

/**
 * After a move has been decided as the best for the current board, that move will be sent here, to the output
 * translation algorithm. This effectively does the reverse of the InputLexer/InputAnalyzer classes, and will convert
 * the move to a human-readable output so the player can determine the best move for the situation.
 */
public class OutputTranslationUnit {
    private static final String[] TYPE_STR = new String[]{"", "N", "B", "R", "Q", "K"};


    public String translate(Move move) {
        if (move.getCastledRook() != Move.CastleSide.NONE) {
            return move.getCastledRook() == Move.CastleSide.KING_SIDE ? "0-0" : "0-0-0";
        }
        String fromSquare = getSquare(move.getFromMask());
        String toSquare = getSquare(move.getToMask());

        // Lazy translation, but fully functional
        return TYPE_STR[move.getMovedPieceType().ordinal()] + fromSquare + toSquare;
    }


    public String getSquare(long mask) {
        if (mask == 0) {
            throw new IllegalAccessError("OTU Failure");
        }
        long fileA = 0x0101010101010101L;

        long fileMask = mask;
        char file = 'a';

        while ((fileMask & fileA) == 0) {
            fileMask = fileMask >>> 1;
            file += 1;
        }

        int rank = 0;
        while (mask != 0) {
            mask = mask >>> 8;
            rank += 1;
        }

        return String.format("%c%d", file, rank);
    }
}
