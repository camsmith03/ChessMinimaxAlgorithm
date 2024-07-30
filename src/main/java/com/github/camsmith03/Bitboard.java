package com.github.camsmith03;

/**
 * <p>
 * Bitboard implemented using longs that represent a 64 square chess board (1 for occupied, 0 for empty). Highly more
 * efficient than other approaches as far less space is consumed by the game board.
 * </p>
 * <p>
 * Follows standard Bitboard conventions:
 * <ul><li>LSB: Square a1 => (0, 0)</li><li>MSB: Square h8 => (7, 7) </li></ul>
 * </p>
 * <p>
 * Stuff to talk about here.
 * </p>
 *
 * @author Cameron Smith
 * @version 07.26.2024
 */
public class Bitboard {
    private final long[] whiteBoards = new long[6];
    private final long[] blackBoards = new long[6];
    private final Piece.Type[] pieceTypeArr;
    private final long[] enPassantFrom = new long[2];
    private final long[] enPassantTo = new long[2];
    private static final long alternatingByteMask = 0xFF00FF00FF00FF00L;

    /* WHITE DEFAULT KING MASKS */
    private static final long whiteKingUpMask    = 0x1010101010101000L;
    private static final long whiteKingDownMask  = 0;
    private static final long whiteKingLeftMask  = 0x000000000000000FL;
    private static final long whiteKingRightMask = 0x00000000000000E0L;
    private static final long whiteKingDiagUL    = 0x0000000102040800L;
    private static final long whiteKingDiagUR    = 0x0000000080402000L;
    private static final long whiteKingDiagLL    = 0;
    private static final long whiteKingDiagLR    = 0;

    /* BLACK DEFAULT KING MASKS */
    private static final long blackKingUpMask    = 0;
    private static final long blackKingDownMask  = 0x0010101010101010L;
    private static final long blackKingLeftMask  = 0x0F00000000000000L;
    private static final long blackKingRightMask = 0xE000000000000000L;
    private static final long blackKingDiagUL    = 0;
    private static final long blackKingDiagUR    = 0;
    private static final long blackKingDiagLL    = 0x0008040201000000L;
    private static final long blackKingDiagLR    = 0x0020408000000000L;


    private final long[][] kingMasks = new long[2][]; // used for ordinal color values
    private enum KingMoveDir { UP, DOWN, LEFT, RIGHT, UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT };

    private final long[][] colorPieceBoards = new long[2][];
    public long gameBoard;
    public long enPassantBoard;
    public final long[] castleMasks = new long[2];



    public Bitboard() {
        // White pieces corresponding to long bitboards
        whiteBoards[0] = 0x000000000000FF00L; // White Pawns
        whiteBoards[1] = 0x0000000000000042L; // White Knights
        whiteBoards[2] = 0x0000000000000024L; // White Bishops
        whiteBoards[3] = 0x0000000000000081L; // White Rooks
        whiteBoards[4] = 0x0000000000000008L; // White Queen
        whiteBoards[5] = 0x0000000000000010L; // White King

        // Black pieces corresponding to long bitboards
        blackBoards[0] = 0x00FF000000000000L; // Black Pawns
        blackBoards[1] = 0x4200000000000000L; // Black Knights
        blackBoards[2] = 0x2400000000000000L; // Black Bishops
        blackBoards[3] = 0x8100000000000000L; // Black Rooks
        blackBoards[4] = 0x0800000000000000L; // Black Queen
        blackBoards[5] = 0x1000000000000000L; // Black King

        enPassantBoard = 0;
        // white en passant pawn move
        enPassantFrom[0] = 0x0000FF00L;
        enPassantTo[0] =   0xFF000000L;
        // black en passant pawn move
        enPassantFrom[1] = 0x000000FF00000000L;
        enPassantTo[1] =   0x00FF000000000000L;

        castleMasks[0] = 0x0000000000000081L; // White Castle Masking Bits
        castleMasks[1] = 0x8100000000000000L; // Black Castle Masking Bits

        colorPieceBoards[0] = whiteBoards;
        colorPieceBoards[1] = blackBoards;

        pieceTypeArr = Piece.Type.values();

        // White King Default Masks
        kingMasks[0] =  new long[]{whiteKingUpMask, whiteKingDownMask, whiteKingLeftMask, whiteKingRightMask,
                whiteKingDiagUL, whiteKingDiagUR, whiteKingDiagLL, whiteKingDiagLR};

        // Black King Default Masks
        kingMasks[1] = new long[]{blackKingUpMask, blackKingDownMask, blackKingLeftMask, blackKingRightMask,
                blackKingDiagUL, blackKingDiagUR, blackKingDiagLL, blackKingDiagLR};


    }

    /**
     * Moves a Piece from an initial position to a final position. Does not check for move legality. This will lead
     * to unexpected behavior without control over legal/illegal moves.
     *
     * @param move
     *      Move to apply to the board
     */
    public void movePiece(Move move) {
        int boardIndex = move.getMovedType().ordinal();
        int colorIndex = move.getMovedColor().ordinal();
        long from = move.getFromMask();
        long to = move.getToMask();
        enPassantBoard = 0; // reset enPassantBoard back to default

        if (move.getCastledRook() != 0) {
            castlePiece(move);
            return;
        }

        int capture = move.getCapturedPieceType().ordinal();

        if (capture != 6) {
            colorPieceBoards[1 - colorIndex][capture] ^= to; // update opponent board to removed captured piece
        }
        else if (move.getMovedType() == Piece.Type.PAWN) {
            if (move.getPromotedType() != Piece.Type.NONE) {
                // if pawn was promoted
                updatePawnPromotion(move);
            }
            else {
                // otherwise, check if pawn move changed possible en passant captures
                checkEnPassantBoard(colorIndex, from, to);
            }
        }
        else if (move.getMovedType() == Piece.Type.KING) {
            castleMasks[colorIndex] = 0;
            updateKingMasks(move);
        }
        else if (move.getMovedType() == Piece.Type.ROOK && castleMasks[colorIndex] != 0) {
            castleMasks[colorIndex] ^= from;
        }

        colorPieceBoards[colorIndex][boardIndex] ^= from | to;
        gameBoard ^= from | to;
    }






    // TODO: Castling on move generator does not indicate king movement, update this to reflect direction so masking
    //       rules can still apply. (i.e.: set from and to)
    public void castlePiece(Move move) {
        if (move.getCastledRook() == 0)
            throw new IllegalArgumentException("Move must be a castled move");

        if (move.getMovedColor() == Piece.Color.WHITE) {
            castleMasks[0] = 0; // indicate that white can no longer castle
            whiteBoards[3] ^= move.getCastledRook();
            if ((move.getCastledRook() & 0x01) == 0) {
                // Castled Queen side
                whiteBoards[3] |= 0x08;
                whiteBoards[5] = 0x04;
            }
            else {
                // Castled King side
                whiteBoards[3] |= 0x20;
                whiteBoards[5] = 0x40;
            }
        }
        else {
            castleMasks[1] = 0; // indicate that black can no longer castle
            blackBoards[3] ^= move.getCastledRook(); // remove the rook that was used for the castling
            if ((move.getCastledRook() & 0x0100000000000000L) == 0) {
                // Castled Queen side
                blackBoards[3] |= 0x0800000000000000L; // add the castled rooks new location
                blackBoards[5] = 0x0400000000000000L;  // add the king's location
            }
            else {
                // Castled King side
                blackBoards[3] |= 0x2000000000000000L; // add the castled rooks new location
                blackBoards[5] = 0x4000000000000000L;  // add the king's location
            }
        }
        updateKingMasks(move);
    }

    /**
     * Converts a Square to a mask that can be used for any of the bitboard to obtain the value of the given
     * position.
     *
     * @param pos
     *      Square to convert
     * @return long
     */
    public long posToMask(Square pos) {
        long yShift = 0x01L << (8 * pos.getY());
        return yShift << pos.getX();
    }


    /**
     * When given a board position, this will return the game piece that corresponds to any piece that may exist at that
     * coordinate, otherwise it will return null.
     *
     * @param mask
     *      bit mask to check for a piece's location of.
     * @return Piece if exists, null otherwise
     */
    public Piece getPiece(long mask) {
        if ((getGameBoard() & mask) != 0) {
            Piece.Color color;
            long[] pieceBitboards;

            if ((getBoardColor(Piece.Color.WHITE) & mask) != 0) {
                color = Piece.Color.WHITE;
                pieceBitboards = whiteBoards;
            }
            else {
                color = Piece.Color.BLACK;
                pieceBitboards = blackBoards;
            }

            for (int i = 0; i < 6; i++) {
                if ((pieceBitboards[i] & mask) != 0) {
                    return new Piece(pieceTypeArr[i], color); // piece type same as bitboard index
                }
            }

            throw new IllegalStateException(); // thrown in any impossible states that would otherwise be ignored by
                                               // null return
        }

        return null; // no piece at that position
    }

    private void updatePawnPromotion(Move move) {
        int promotedType = move.getPromotedType().ordinal();
        int colorIndex = move.getMovedColor().ordinal();
        long from = move.getFromMask();
        long to = move.getToMask();

        if (move.getCapturedPieceType() != Piece.Type.NONE) {
            // captured piece and promoted pawn
            colorPieceBoards[1 - colorIndex][move.getCapturedPieceType().ordinal()] ^= to; // remove the captured piece
        }

        colorPieceBoards[colorIndex][0] ^= from; // remove the pawn
        colorPieceBoards[colorIndex][promotedType] |= to; // add the promoted piece
    }

    private void checkEnPassantBoard(int colorIndex, long from, long to) {
        if ((from & enPassantFrom[colorIndex]) != 0) {
            if ((to & enPassantTo[colorIndex]) != 0) {
                long mask;
                if ((to & alternatingByteMask) != 0) {
                    mask = alternatingByteMask;
                }
                else { mask = ~alternatingByteMask; }

                if (((to << 1) & mask) != 0) {
                    enPassantBoard |= (to << 1) & colorPieceBoards[1 - colorIndex][0];
                }

                if (((to >>> 1 ) & mask) != 0) {
                    enPassantBoard |= (to >>> 1) & colorPieceBoards[1 - colorIndex][0];
                }
                enPassantBoard |= to;
            }
        }
    }

    /**
     * Deprecated, use getPieceBitboards(Piece.Color) instead
     *
     * @return long[]
     */
    @Deprecated
    public long[] getWhitePieceBitboards() {
        return whiteBoards;
    }

    /**
     * Deprecated, use getPieceBitboards(Piece.Color) instead
     *
     * @return long[]
     */
    @Deprecated
    public long[] getBlackPieceBitboards() {
        return blackBoards;
    }

    public long[] getPieceBitboards(Piece.Color color) {
        return colorPieceBoards[color.ordinal()];
    }

    public long getGameBoard() {
        long gameBoard = 0;

        for (int i = 0; i < 6; i++) {
            gameBoard |= whiteBoards[i] | blackBoards[i];
        }
        return gameBoard;
    }

    public long getBoardColor(Piece.Color color) {
        long boardColor = 0;
        int colorIndex = color.ordinal();
        for (int i = 0; i < 6; i++) {
            boardColor |= colorPieceBoards[colorIndex][i];
        }

        return boardColor;
    }

    // Keep private, lack of index check not safe
    private long getBoardColorIndex(int colorIndex) {
        long boardColor = 0;
        for (int i = 0; i < 6; i++) {
            boardColor |= colorPieceBoards[colorIndex][i];
        }

        return boardColor;
    }

    /**
     *
     *
     */
    public boolean isKingInCheck(Piece.Color color) {
        return kingMaskCheck(color.ordinal()) || knightCheck(color.ordinal());
    }


    /**
     * <p>
     * When passed a color's ordinal value (i.e.: index), this will determine if that color's king is in a checked
     * position. The masks are initialized to their default values, and transformed during any king moves during the
     * game.
     * </p>
     *
     * @param colorIndex
     *      Color ordinal value of king to inspect.
     * @return true if king is in check; false otherwise.
     */
    private boolean kingMaskCheck(int colorIndex) {

        long colorBoard = getBoardColorIndex(colorIndex);
        long oppColorBoard = getBoardColorIndex(1 - colorIndex);
        long kUp    = kingMasks[colorIndex][0];
        long kDown  = kingMasks[colorIndex][1];
        long kLeft  = kingMasks[colorIndex][2];
        long kRight = kingMasks[colorIndex][3];

        long diagUL = kingMasks[colorIndex][4];
        long diagUR = kingMasks[colorIndex][5];
        long diagLL = kingMasks[colorIndex][6];
        long diagLR = kingMasks[colorIndex][7];

        long kUpPiece = (kUp & gameBoard) & -(kUp & gameBoard);
        if ((colorBoard & kUpPiece) == 0) {
            // King is compromised above.

            // Check to see if opposing piece is above
            if ((oppColorBoard & kUpPiece) != 0) {
                // If above, only candidates for check are Queen or Rook
                return (kUpPiece & colorPieceBoards[1 - colorIndex][3]) != 0 // piece is a rook
                    || (kUpPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
            }
        }

        long kDownPieces = kDown & gameBoard;
        if ((colorBoard & kDownPieces) < (oppColorBoard & kDownPieces)) {
            // if our pieces are less than the opp pieces (for kDown), the opp piece is higher up than ours.

            // We know opposing piece is below, but need to check which one.
            // TODO: Determine how to get most significant opposing piece that we are vulnerable to.
        }


        long diagURPiece = (diagUR & gameBoard) & -(diagUR & gameBoard);
        if ((colorBoard & diagURPiece) == 0) {
            // King is compromised on the upper right diagonal

            // Check to see if opposing piece on diagonal
            if ((oppColorBoard & diagURPiece) != 0) {
                // See if opposing color piece has a check on King

                // Can be a pawn if diagULPiece == kingPos >> 9 (for white or black)
                if (diagURPiece == (colorPieceBoards[colorIndex][5] >> 9)) {
                    return (diagURPiece & colorPieceBoards[1 - colorIndex][0]) != 0 // piece is a pawn
                        || (diagURPiece & colorPieceBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                        || (diagURPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
                }

                // Can be a Bishop or Queen in any compromised square
                return (diagURPiece & colorPieceBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                    || (diagURPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen


            }
        }

        long diagULPiece = (diagUL & gameBoard) & -(diagUL & gameBoard);
        if ((colorBoard & diagULPiece) == 0) {
            // King is compromised on the upper left diagonal

            // Check to see if opposing piece on diagonal
            if ((oppColorBoard & diagULPiece) != 0) {
                // See if opposing color piece has attack on King

                // Can be a pawn if diagULPiece == kingPos >> 7 (for white or black)
                if (diagULPiece == (colorPieceBoards[colorIndex][5] >> 7)) {
                    return (diagULPiece & colorPieceBoards[1 - colorIndex][0]) != 0 // piece is a pawn
                        || (diagULPiece & colorPieceBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                        || (diagULPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
                }

                // Can be a Bishop or Queen in any compromised square
                return (diagULPiece & colorPieceBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                    || (diagULPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen

            }
        }


        // Check to see if the opponent has knights on the board. If so, call knightCheck
        return colorPieceBoards[1 - colorIndex][1] == 0 || knightCheck(colorIndex);
    }

    /**
     * <p>
     * Updates each of king's masks corresponding to the color of the move. This method will be called iff the king from
     * either side is moved, whether by castling or normal means.
     * </p><p>
     * These masks represent the vulnerable squares based on directions the king could be attacked from. When used with
     * isKingInCheck(), the process of seeing whether an attacking piece has a check position on the king becomes
     * trivial.
     * <br>Note that checks with enemy knights were calculated in a separate method.
     * </p><p>
     * The bitwise calculations to determine mask transformations are complicated, but local variables were added to aid
     * in interpretation.
     * </p>
     *
     * @param move
     *      Move from either king
     */
    private void updateKingMasks(Move move) {
        int colorIndex = move.getMovedColor().ordinal();
        long oldKingPos = move.getFromMask();
        long newKingPos = move.getToMask();

        long rightBoundXor = 0x0101010101010101L;  // illegal col 0 for diagLR and diagUR
        long leftBoundXor  = 0x8080808080808080L;  // illegal col 7 for diagLL and diagUL

        KingMoveDir moveDirection = findKingDirection(oldKingPos, newKingPos);

        long[] kingMask = kingMasks[colorIndex];

        // local variables to aid in readability
        long kUp     = kingMask[0]; // mask UP
        long kDown   = kingMask[1]; // mask DOWN
        long kLeft   = kingMask[2]; // mask LEFT
        long kRight  = kingMask[3]; // mask RIGHT

        long diagUL  = kingMask[4]; // mask UPPER LEFT
        long diagUR  = kingMask[5]; // mask UPPER RIGHT
        long diagLL  = kingMask[6]; // mask LOWER LEFT
        long diagLR  = kingMask[7]; // mask LOWER RIGHT

        if (moveDirection == KingMoveDir.UP) {

            kingMask[0] = oldKingPos | kUp;
            kingMask[1] = newKingPos ^ kDown;

            kingMask[2] = kLeft  << 8;
            kingMask[3] = kRight << 8;

            kingMask[4] = diagUL << 8;
            kingMask[5] = diagUR << 8;

            kingMask[6] = (oldKingPos >>> 1) | (diagLL << 8);
            kingMask[7] = (oldKingPos << 1)  | (diagLR << 8);

        }
        else if (moveDirection == KingMoveDir.DOWN) {

            kingMask[0] = newKingPos ^ kUp;
            kingMask[1] = oldKingPos | kDown;

            kingMask[2] = kLeft  >>> 8;
            kingMask[3] = kRight >>> 8;

            kingMask[4] = (oldKingPos >>> 1) | (diagUL >>> 8);
            kingMask[5] = (oldKingPos << 1)  | (diagUR >>> 8);

            kingMask[6] = diagLL >>> 8;
            kingMask[7] = diagLR >>> 8;

        }
        else if (moveDirection == KingMoveDir.LEFT) {

            kingMask[0] = kUp   >>> 1;
            kingMask[1] = kDown >>> 1;

            kingMask[2] = kLeft  ^ newKingPos;
            kingMask[3] = kRight | oldKingPos;

            kingMask[4] = (diagUL >>> 1) ^ leftBoundXor;
            kingMask[5] = (oldKingPos | diagUR) << 8;

            kingMask[6] = (diagLL >>> 1) ^ leftBoundXor;
            kingMask[7] =  (oldKingPos | diagLR) >>> 8;

        }
        else if (moveDirection == KingMoveDir.RIGHT) {

            kingMask[0] = kUp   << 1;
            kingMask[1] = kDown << 1;

            kingMask[2] = kLeft | oldKingPos;
            kingMask[3] = kRight ^ newKingPos;

            kingMask[4] = (oldKingPos | diagUL) << 8;
            kingMask[5] = (diagUR << 1) ^ rightBoundXor;

            kingMask[6] = (oldKingPos | diagLL) >>> 8;
            kingMask[7] = (diagLR << 1) ^ rightBoundXor;

        }
        else if (moveDirection == KingMoveDir.UPPER_LEFT) {

            kingMask[0] = (kUp >>> 1) ^ newKingPos;
            kingMask[1] = (kDown | oldKingPos) >>> 1;

            kingMask[2] = (kLeft << 8) ^ newKingPos;
            kingMask[3] = (kRight | oldKingPos) << 8;

            kingMask[4] = diagUL ^ newKingPos;
            kingMask[5] = (oldKingPos | diagUR) << 16;

            kingMask[6] = ((oldKingPos | diagLL) >>> 2) ^ (leftBoundXor | (leftBoundXor >>> 1));
            kingMask[7] = diagLR | oldKingPos;

        }
        else if (moveDirection == KingMoveDir.UPPER_RIGHT) {

            kingMask[0] = (kUp << 1) ^ newKingPos;
            kingMask[1] = (kDown | oldKingPos) << 1;

            kingMask[2] = (kLeft | oldKingPos) << 8;
            kingMask[3] = (kRight << 8) ^ newKingPos;

            kingMask[4] = (oldKingPos | diagUL) << 16;
            kingMask[5] = diagUR ^ newKingPos;

            kingMask[6] = diagLL | oldKingPos;
            kingMask[7] = ((oldKingPos | diagLR) << 2) ^ (rightBoundXor | (rightBoundXor << 1));

        }
        else if (moveDirection == KingMoveDir.LOWER_LEFT) {

            kingMask[0] = (kUp | oldKingPos) >>> 1;
            kingMask[1] = (kDown >>> 1) ^ newKingPos;

            kingMask[2] = (kLeft >>> 8) ^ newKingPos;
            kingMask[3] = (kRight | oldKingPos) >>> 8;

            kingMask[4] = ((oldKingPos | diagUL) >>> 2) ^ (leftBoundXor | (leftBoundXor >>> 1));
            kingMask[5] = oldKingPos | diagUR;

            kingMask[6] = diagLL ^ newKingPos;
            kingMask[7] = (oldKingPos | diagLR) >>> 16;

        }
        else {// moveDirection == KingMoveDir.LOWER_RIGHT

            kingMask[0] = (kUp | oldKingPos) << 1;
            kingMask[1] = (kDown << 1) ^ newKingPos;

            kingMask[2] = (kLeft | oldKingPos) >>> 8;
            kingMask[3] = (kRight >>> 8) ^ newKingPos;

            kingMask[4] = ((oldKingPos | diagUR) << 2) ^ (rightBoundXor | (rightBoundXor << 1));
            kingMask[5] = diagUL | oldKingPos;

            kingMask[6] = diagLR ^ newKingPos;
            kingMask[7] = (oldKingPos | diagLL) >>> 16;

        }
    }

    // TODO: Implement this
    private KingMoveDir findKingDirection(long from, long to) {
        return KingMoveDir.UP;
    }

    /**
     * Checks if king is in check to any opposing knights that are outside the king boundary.
     * TODO: Implement this
     */
    private boolean knightCheck(int colorIndex) {
        return false;
    }
}