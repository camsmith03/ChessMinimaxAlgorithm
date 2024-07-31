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
    private static final long whiteKingUpMask     = 0x1010101010101000L;
    private static final long whiteKingDownMask   = 0;
    private static final long whiteKingLeftMask   = 0x000000000000000FL;
    private static final long whiteKingRightMask  = 0x00000000000000E0L;
    private static final long whiteKingDiagUL     = 0x0000000102040800L;
    private static final long whiteKingDiagUR     = 0x0000000080402000L;
    private static final long whiteKingDiagLL     = 0;
    private static final long whiteKingDiagLR     = 0;
    private static final long whiteKingKnightMask = 0x0000000000284400L;

    /* BLACK DEFAULT KING MASKS */
    private static final long blackKingUpMask     = 0;
    private static final long blackKingDownMask   = 0x0010101010101010L;
    private static final long blackKingLeftMask   = 0x0F00000000000000L;
    private static final long blackKingRightMask  = 0xE000000000000000L;
    private static final long blackKingDiagUL     = 0;
    private static final long blackKingDiagUR     = 0;
    private static final long blackKingDiagLL     = 0x0008040201000000L;
    private static final long blackKingDiagLR     = 0x0020408000000000L;
    private static final long blackKingKnightMask = 0x0044280000000000L;

    private final long[][] defaultKingSideCastleMasks  = new long[2][];
    private final long[][] defaultQueenSideCastleMasks = new long[2][];

    private final long[][] kingMasks              = new long[2][];
    private enum KingMoveDir { UP, DOWN, LEFT, RIGHT, UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT };

    private final long[][] colorPieceBoards = new long[2][];
    public long gameBoard;
    public long enPassantBoard;
    public final long[] castleCheckMasks = new long[2];



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

        castleCheckMasks[0] = 0x0000000000000081L; // White Castle Masking Bits
        castleCheckMasks[1] = 0x8100000000000000L; // Black Castle Masking Bits

        colorPieceBoards[0] = whiteBoards;
        colorPieceBoards[1] = blackBoards;

        pieceTypeArr = Piece.Type.values();

        // White King Default Masks
        kingMasks[0] =  new long[]{whiteKingUpMask, whiteKingDownMask, whiteKingLeftMask, whiteKingRightMask,
                whiteKingDiagUL, whiteKingDiagUR, whiteKingDiagLL, whiteKingDiagLR, whiteKingKnightMask};

        // Black King Default Masks
        kingMasks[1] = new long[]{blackKingUpMask, blackKingDownMask, blackKingLeftMask, blackKingRightMask,
                blackKingDiagUL, blackKingDiagUR, blackKingDiagLL, blackKingDiagLR, blackKingKnightMask};

        long[] whiteKingSideMasks = new long[]{
                whiteKingUpMask    << 2,
                0, // kDown
                whiteKingLeftMask  |  0x30,
                whiteKingRightMask ^  0x60,
                0x000102040810200L, // diagUL
                0x000000000008000L, // diagUR
                0, // diagLL
                0, // diagLR
                0x0000000000A1000L // knights
        };

        long[] whiteQueenSideMasks = new long[]{
                whiteKingUpMask    >>> 2,
                0, // kDown
                whiteKingLeftMask  ^ 0x0C,
                whiteKingRightMask | 0x18,
                0x0000000000010200L, // diagUL
                0x0000804020100800L, // diagUR
                0, // diagLL
                0, // diagLR
                0x0000000000A1100L, // knights
        };

        long[] blackKingSideMasks = new long[]{
                0, // kUp
                blackKingDownMask  << 2,
                blackKingLeftMask  | 0x3000000000000000L,
                0x8000000000000000L, // kRight
                0, // diagUL
                0, // diagUR
                0x0020100804020100L, // diagLL
                0x0080000000000000L, // diagLR
                0x0010A00000000000L  // knights
        };

        long[] blackQueenSideMasks = new long[] {
                0, // kUp
                blackKingDownMask  >>> 2,
                0x0300000000000000L, // kLeft
                blackKingRightMask | 0x1800000000000000L,
                0, // diagUL
                0, // diagUR
                0x0002010000000000L, // diagLL
                0x0008102040800000L, // diagLR
                0x00110A0000000000L  // knights
        };

        defaultKingSideCastleMasks[0]  = whiteKingSideMasks;
        defaultKingSideCastleMasks[1]  = blackKingSideMasks;
        defaultQueenSideCastleMasks[0] = whiteQueenSideMasks;
        defaultQueenSideCastleMasks[1] = blackQueenSideMasks;

    }

    /**
     * Moves a Piece from an initial position to a final position.
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

        if (move.getCastledRook() != Move.CastleSide.NONE) {
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
            castleCheckMasks[colorIndex] = 0;
            updateKingMasks(move);
        }
        else if (move.getMovedType() == Piece.Type.ROOK && castleCheckMasks[colorIndex] != 0) {
            castleCheckMasks[colorIndex] ^= from;
        }

        colorPieceBoards[colorIndex][boardIndex] ^= from | to;
        gameBoard ^= from | to;
    }






    // TODO: Castling on move generator does not indicate king movement, update this to reflect direction so masking
    //       rules can still apply. (i.e.: set from and to)
    private void castlePiece(Move move) {
        if (move.getCastledRook() == Move.CastleSide.NONE)
            throw new IllegalArgumentException("Move must be a castled move");

        if (move.getMovedColor() == Piece.Color.WHITE) {
            castleCheckMasks[0] = 0; // indicate that white can no longer castle
            if (move.getCastledRook() == Move.CastleSide.QUEEN_SIDE) {
                // Castled Queen side
                whiteBoards[3] ^= 0x01;
                whiteBoards[3] |= 0x08;
                whiteBoards[5] = 0x04;
            }
            else {
                // Castled King side
                whiteBoards[3] ^= 0x80;
                whiteBoards[3] |= 0x20;
                whiteBoards[5] = 0x40;
            }
        }
        else {
            castleCheckMasks[1] = 0; // indicate that black can no longer castle
            if (move.getCastledRook() == Move.CastleSide.QUEEN_SIDE) {
                // Castled Queen side
                blackBoards[3] ^= 0x0100000000000000L; // remove original rook
                blackBoards[3] |= 0x0800000000000000L; // add the castled rooks new location
                blackBoards[5] = 0x0400000000000000L;  // add the king's location
            }
            else {
                // Castled King side
                blackBoards[3] ^= 0x8000000000000000L; // remove original rook
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
        return kingMaskCheck(color.ordinal());
    }

    public boolean doesMovePutKingInCheck(Move move) {
        return false; // TODO: Virtually apply a move to test if a king will be in check, without actually affecting bitboard.
    }


    /**
     * <p>
     * When passed a color's ordinal value (i.e.: index), this will determine if that color's king is in a checked
     * position. The masks are initialized to their default values, and only transformed for any king moves during the
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
        long kUp     = kingMasks[colorIndex][0];
        long kDown   = kingMasks[colorIndex][1];
        long kLeft   = kingMasks[colorIndex][2];
        long kRight  = kingMasks[colorIndex][3];

        long diagUL  = kingMasks[colorIndex][4];
        long diagUR  = kingMasks[colorIndex][5];
        long diagLL  = kingMasks[colorIndex][6];
        long diagLR  = kingMasks[colorIndex][7];
        long knights = kingMasks[colorIndex][8];

        long kUpPiece = (kUp & gameBoard) & -(kUp & gameBoard);
        if ((colorBoard & kUpPiece) == 0 && (oppColorBoard & kUpPiece) != 0) {
            // See if king is vulnerable above and check to see if an opposing piece of threat is above

            // Only candidates for check are enemy Queen or Rook
            return (kUpPiece & colorPieceBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (kUpPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen

        }

        long kDownPieces = kDown & gameBoard;
        if ((colorBoard & kDownPieces) < (oppColorBoard & kDownPieces)) {
            // if our pieces are less than the opp pieces (for kDown), the opp piece is higher up than ours.

            // We know opposing piece is below, but need to check which one is of direct threat along the attack vector.
            long kingPos = colorPieceBoards[colorIndex][5];
            long pieceOfThreat = getMSB(oppColorBoard & kDownPieces, kingPos, 8);

            // Only candidates of threat are Rook or Queen
            return (pieceOfThreat & colorPieceBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (pieceOfThreat & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long kLeftPieces = kLeft & gameBoard;
        if ((colorBoard & kLeftPieces) < (oppColorBoard & kLeftPieces)) {
            // opponent has a piece in front of our own along the attack vector.

            // find the position of the piece of direct threat to the king
            long kingPos = colorPieceBoards[colorIndex][5];
            long pieceOfThreat = getMSB(kLeftPieces, kingPos, 1);

            // Only candidates of threat are Rook or Queen
            return (pieceOfThreat & colorPieceBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (pieceOfThreat & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long kRightPiece = (kRight & gameBoard) & -(kRight & gameBoard);
        if ((colorBoard & kRightPiece) == 0 && (oppColorBoard & kRightPiece) != 0) {
            // King is vulnerable to the right, and an enemy piece is of direct threat

            // Only candidates of threat are Rook or Queen
            return (kRightPiece & colorPieceBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (kRightPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
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

        long diagURPiece = (diagUR & gameBoard) & -(diagUR & gameBoard);
        if ((colorBoard & diagURPiece) == 0 && (oppColorBoard & diagURPiece) != 0) {
            // King is compromised on the upper right diagonal and opposing piece on diagonal

            // Can be a pawn if diagULPiece == kingPos >> 9 (for white or black)
            if (diagURPiece == (colorPieceBoards[colorIndex][5] >> 9)) {
                return (diagURPiece & colorPieceBoards[1 - colorIndex][0]) != 0 // piece is a pawn
                    || (diagURPiece & colorPieceBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                    || (diagURPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
            }

            // Can be a Bishop or Queen in any other compromised square
            return (diagURPiece & colorPieceBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                || (diagURPiece & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long diagLLPieces = diagLL & gameBoard;
        if ((colorBoard & diagLLPieces) < (oppColorBoard & diagLLPieces)) {
            // King is exposed on the lower left diagonal.

            long kingPos = colorPieceBoards[colorIndex][5];
            long pieceOfThreat = getMSB(oppColorBoard & diagLLPieces, kingPos, 9);

            // King only in check to enemy bishop or queen on the attack vector
            return (pieceOfThreat & colorPieceBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                || (pieceOfThreat & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long diagLRPieces = diagLR & gameBoard;
        if ((colorBoard & diagLRPieces) < (oppColorBoard & diagLRPieces)) {
            // King is exposed on the lower right diagonal.

            long kingPos = colorPieceBoards[colorIndex][5];
            long pieceOfThreat = getMSB(oppColorBoard & diagLRPieces, kingPos, 7);

            // King only in check to enemy bishop or queen along the attack vector
            return (pieceOfThreat & colorPieceBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                || (pieceOfThreat & colorPieceBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        // Check to see if any of the opponents knights have a check position on the king.
        return (colorPieceBoards[1 - colorIndex][1] & knights) != 0;
    }

    /**
     * <p>
     * Acquires the MSB from a mask relating to a kings potential vulnerable squares. The step size relates to the
     * direction of the kingMask, from the king's starting square. The MSB for the mask is associated with the first
     * piece closest to the king along the direction of vulnerability.
     * </p><p>
     * Note, usage requires a kingMask != 0. In other words, there must be a 1 following in the direction of the step
     * size, otherwise an exception will be thrown.
     * </p><p>
     * These are the only permitted mask directions as well as their associated step size:
     * <ul>
     * <li>Down: step = 8</li>
     * <li>Left: step = 1</li>
     * <li>Lower-Left Diagonal: step = 9</li>
     * <li>Lower-Right Diagonal: step = 7</li>
     * </ul></p>
     *
     * @param kingMask
     *      Mask associated with a permitted direction (without king bit)
     * @param kingPos
     *      Single Bit for vulnerable king's position
     * @param step
     *      Right shift step size based on direction from the kings position
     * @return Mask for piece of direct threat in the given direction
     */
    private long getMSB(long kingMask, long kingPos, int step) {
        if ((kingPos & kingMask) != 0) {
            return kingPos & kingMask;
        }

        if (kingMask == 0 || kingPos == 0) throw new IllegalStateException("Invalid king mask");

        return getMSB(kingMask, kingPos >>> step, step);
    }


    /**
     * <p>
     * Updates each of king's masks corresponding to the color of the move. This method will be called iff the king from
     * either side is moved, whether by castling or normal means.
     * </p><p>
     * These masks represent the vulnerable squares based on directions the king could be attacked from. When used with
     * isKingInCheck(), the process of seeing whether an attacking piece has a check position on the king becomes
     * trivial.
     * </p><p>
     * The bitwise calculations to determine mask transformations are complicated, but local variables were added to aid
     * in interpretation.
     * </p>
     *
     * @param move
     *      Move from either king
     */
    private void updateKingMasks(Move move) {
        if (move.getCastledRook() != Move.CastleSide.NONE) {
            updateKingMasksCastle(move); // castling moves passed to separate method, as behavior is separate
            return; // warning, does not check if castling has already been done
        }

        int colorIndex = move.getMovedColor().ordinal();
        long oldPos = move.getFromMask();
        long newPos = move.getToMask();

        long rightBoundXor = 0x0101010101010101L;  // illegal col 0 for diagLR and diagUR
        long leftBoundXor  = 0x8080808080808080L;  // illegal col 7 for diagLL and diagUL

        KingMoveDir moveDirection = findKingDirection(oldPos, newPos);

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

        long knights = kingMask[8]; // mask KNIGHT SQUARES

        // TODO: Update so castling correctly adjusts masks!!
        if (moveDirection == KingMoveDir.UP) {

            kingMask[0] = oldPos | kUp;
            kingMask[1] = newPos ^ kDown;

            kingMask[2] = kLeft  << 8;
            kingMask[3] = kRight << 8;

            kingMask[4] = diagUL << 8;
            kingMask[5] = diagUR << 8;

            kingMask[6] = (oldPos >>> 1) | (diagLL << 8);
            kingMask[7] = (oldPos << 1)  | (diagLR << 8);

            kingMask[8] = (knights << 8) | (oldPos << 2) | (oldPos >>> 2) | (oldPos >>> 7) | (oldPos >>> 9);

        }
        else if (moveDirection == KingMoveDir.DOWN) {

            kingMask[0] = newPos ^ kUp;
            kingMask[1] = oldPos | kDown;

            kingMask[2] = kLeft  >>> 8;
            kingMask[3] = kRight >>> 8;

            kingMask[4] = (oldPos >>> 1) | (diagUL >>> 8);
            kingMask[5] = (oldPos << 1)  | (diagUR >>> 8);

            kingMask[6] = diagLL >>> 8;
            kingMask[7] = diagLR >>> 8;

            kingMask[8] = (knights >>> 8) | (oldPos << 2) | (oldPos >>> 2) | (oldPos << 7) | (oldPos << 9);

        }
        else if (moveDirection == KingMoveDir.LEFT) {

            kingMask[0] = kUp   >>> 1;
            kingMask[1] = kDown >>> 1;

            kingMask[2] = kLeft  ^ newPos;
            kingMask[3] = kRight | oldPos;

            kingMask[4] = (diagUL >>> 1) ^ leftBoundXor;
            kingMask[5] = (oldPos | diagUR) << 8;

            kingMask[6] = (diagLL >>> 1) ^ leftBoundXor;
            kingMask[7] = (oldPos | diagLR) >>> 8;

            kingMask[8] = (knights >>> 1) ^ leftBoundXor;

        }
        else if (moveDirection == KingMoveDir.RIGHT) {

            kingMask[0] = kUp   << 1;
            kingMask[1] = kDown << 1;

            kingMask[2] = kLeft | oldPos;
            kingMask[3] = kRight ^ newPos;

            kingMask[4] = (oldPos | diagUL) << 8;
            kingMask[5] = (diagUR << 1) ^ rightBoundXor;

            kingMask[6] = (oldPos | diagLL) >>> 8;
            kingMask[7] = (diagLR << 1) ^ rightBoundXor;

            kingMask[8] = (knights << 8) ^ rightBoundXor;
        }
        else if (moveDirection == KingMoveDir.UPPER_LEFT) {

            kingMask[0] = (kUp >>> 1) ^ newPos;
            kingMask[1] = (kDown | oldPos) >>> 1;

            kingMask[2] = (kLeft << 8) ^ newPos;
            kingMask[3] = (kRight | oldPos) << 8;

            kingMask[4] = diagUL ^ newPos;
            kingMask[5] = (oldPos | diagUR) << 16;

            kingMask[6] = ((oldPos | diagLL) >>> 2) ^ (leftBoundXor | (leftBoundXor >>> 1));
            kingMask[7] = diagLR | oldPos;

            kingMask[8] = (((knights << 7) | (oldPos >>> 3) | (oldPos >>> 10)) ^ (leftBoundXor | (leftBoundXor >>> 1)))
                    | (oldPos << 24)  | (oldPos >>> 8)
                    | (((oldPos << 1) | (oldPos << 17)) ^ rightBoundXor) ;

        }
        else if (moveDirection == KingMoveDir.UPPER_RIGHT) {

            kingMask[0] = (kUp << 1) ^ newPos;
            kingMask[1] = (kDown | oldPos) << 1;

            kingMask[2] = (kLeft | oldPos) << 8;
            kingMask[3] = (kRight << 8) ^ newPos;

            kingMask[4] = (oldPos | diagUL) << 16;
            kingMask[5] = diagUR ^ newPos;

            kingMask[6] = diagLL | oldPos;
            kingMask[7] = ((oldPos | diagLR) << 2) ^ (rightBoundXor | (rightBoundXor << 1));

            kingMask[8] = (((oldPos >>> 1) | (oldPos << 15)) ^ leftBoundXor)
                    | (oldPos >>> 8) | (oldPos << 24)
                    | (((oldPos << 3) | (knights << 9) | (oldPos >>> 6)) ^ (rightBoundXor | (rightBoundXor << 1)));

        }
        else if (moveDirection == KingMoveDir.LOWER_LEFT) {

            kingMask[0] = (kUp | oldPos) >>> 1;
            kingMask[1] = (kDown >>> 1) ^ newPos;

            kingMask[2] = (kLeft >>> 8) ^ newPos;
            kingMask[3] = (kRight | oldPos) >>> 8;

            kingMask[4] = ((oldPos | diagUL) >>> 2) ^ (leftBoundXor | (leftBoundXor >>> 1));
            kingMask[5] = oldPos | diagUR;

            kingMask[6] = diagLL ^ newPos;
            kingMask[7] = (oldPos | diagLR) >>> 16;

            kingMask[8] = (((knights >>> 9) | (oldPos >>> 3) | (oldPos << 6)) ^ (leftBoundXor | (leftBoundXor >>> 1)))
                    | (oldPos >>> 24) | (oldPos << 8)
                    | (((oldPos << 1) | (oldPos >>> 15)) ^ rightBoundXor);

        }
        else {// moveDirection == KingMoveDir.LOWER_RIGHT

            kingMask[0] = (kUp | oldPos) << 1;
            kingMask[1] = (kDown << 1) ^ newPos;

            kingMask[2] = (kLeft | oldPos) >>> 8;
            kingMask[3] = (kRight >>> 8) ^ newPos;

            kingMask[4] = ((oldPos | diagUR) << 2) ^ (rightBoundXor | (rightBoundXor << 1));
            kingMask[5] = diagUL | oldPos;

            kingMask[6] = diagLR ^ newPos;
            kingMask[7] = (oldPos | diagLL) >>> 16;

            kingMask[8] = (((oldPos >>> 1) | (oldPos >>> 17))  ^ leftBoundXor)
                    | (oldPos << 8) | (oldPos >>> 24)
                    | (((knights >>> 7) | (oldPos << 3) | (oldPos << 10)) ^ (rightBoundXor | rightBoundXor << 1));
        }
    }

    /**
     * Given a from and to position, this returns the KingMoveDir enum corresponding to the king's local axis of
     * movement. If the king's movement is not one a legal movement, an exception will be thrown, as this should never
     * occur under normal circumstances.
     *
     * @param from
     *      Single bit for king's original position.
     * @param to
     *      Single bit for king's new position.
     * @return KingMoveDir
     */
    private KingMoveDir findKingDirection(long from, long to) {
        if (to == (from << 8)) {
            return KingMoveDir.UP;
        }
        else if (to == (from >>> 8)) {
            return KingMoveDir.DOWN;
        }
        else if (to == (from >>> 1)) {
            return KingMoveDir.LEFT;
        }
        else if (to == (from << 1)) {
            return KingMoveDir.RIGHT;
        }
        else if (to == (from << 7)) {
            return KingMoveDir.UPPER_LEFT;
        }
        else if (to == (from << 9)) {
            return KingMoveDir.UPPER_RIGHT;
        }
        else if (to == (from >>> 9)) {
            return KingMoveDir.LOWER_LEFT;
        }
        else if (to == (from >>> 7)) {
            return KingMoveDir.LOWER_RIGHT;
        }

        throw new IllegalStateException("King move was illegal!");
    }


    private void updateKingMasksCastle(Move move) {
        int colorIndex = move.getMovedColor().ordinal();
        if (move.getCastledRook() == Move.CastleSide.QUEEN_SIDE) {
            System.arraycopy(defaultQueenSideCastleMasks[colorIndex], 0, kingMasks[colorIndex], 0, 9);
        }
        else if (move.getCastledRook() == Move.CastleSide.KING_SIDE) {
            System.arraycopy(defaultKingSideCastleMasks[colorIndex], 0, kingMasks[colorIndex], 0, 9);
        }

        throw new IllegalStateException("Passed a non-castled move to the update castle mask method!");
    }
}