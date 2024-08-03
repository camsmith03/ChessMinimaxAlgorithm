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
    private final Piece.Type[] pieceTypeArr = Piece.Type.values();;
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

    private enum KingMoveDir { UP, DOWN, LEFT, RIGHT, UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT };

    public long enPassantBoard = 0;

    private final long[][] tempBoards = new long[2][]; // Copy of boards to allow for virtual nondestructive, movements.

    // Acts as an array of 15 pairs of integers (int colorIndex, int maskIndex)
    private final int[] changeHist = new int[30];
    // The theoretical limit for a single move to affect masking bits is 15, although it is unlikely this would ever
    // approach that limit.
    private int changeHistSize; // essentially the stack pointer to modified boards

    private final long[][] boards = new long[2][];
    private long gameBoard;
    private long whiteBoard;
    private long blackBoard;





    public Bitboard() {
        /* ========================================================== */
        /* ==================  WHITE MASKING BITS  ================== */
        /* ===========================================================*/
        boards[0] = new long[]{
                // #########    PIECE TYPE MASKS      #########
                0x000000000000FF00L, // White Pawns
                0x0000000000000042L, // White Knights
                0x0000000000000024L, // White Bishops
                0x0000000000000081L, // White Rooks
                0x0000000000000008L, // White Queen
                0x0000000000000010L, // White King

                // #########   CASTLING ROOKS MASK    #########
                0x0000000000000081L, // Castle-able Rooks Mask

                // ######### EN PASSANT CAPTURE MASKS #########
                0x000000FF00000000L, // En Passant From Location Masks
                0x00FF000000000000L, // En Passant To Location Masks

                // #########     KING CHECK MASKS     #########
                whiteKingUpMask,     // kUp Mask
                whiteKingDownMask,   // kDown Mask
                whiteKingLeftMask,   // kLeft Mask
                whiteKingRightMask,  // kRight Mask
                whiteKingDiagUL,     // kUpperLeft Mask
                whiteKingDiagUR,     // kUpperRight Mask
                whiteKingDiagLL,     // kLowerLeft Mask
                whiteKingDiagLR,     // kLowerRight Mask
                whiteKingKnightMask  // kKnightPos Mask
        };

        /* ========================================================== */
        /* ==================  BLACK MASKING BITS  ================== */
        /* ===========================================================*/
        boards[1] = new long[]{
                // #########    PIECE TYPE MASKS      #########
                0x00FF000000000000L, // Black Pawns
                0x4200000000000000L, // Black Knights
                0x2400000000000000L, // Black Bishops
                0x8100000000000000L, // Black Rooks
                0x0800000000000000L, // Black Queen
                0x1000000000000000L, // Black King

                // #########   CASTLING ROOKS MASK    #########
                0x8100000000000000L, // Castle-able Rooks Mask

                // ######### EN PASSANT CAPTURE MASKS #########
                0x000000000000FF00L, // En Passant From Location Masks
                0x00000000FF000000L, // En Passant To Location Masks

                // #########     KING CHECK MASKS     #########
                blackKingUpMask,     // kUp Mask
                blackKingDownMask,   // kDown Mask
                blackKingLeftMask,   // kLeft Mask
                blackKingRightMask,  // kRight Mask
                blackKingDiagUL,     // kUpperLeft Mask
                blackKingDiagUR,     // kUpperRight Mask
                blackKingDiagLL,     // kLowerLeft Mask
                blackKingDiagLR,     // kLowerRight Mask
                blackKingKnightMask  // kKnightPos Mask
        };

        // Create a copy of the masks to offer a virtual board that changes can be applied to, then reverted back if the
        // move applied was illegal.
        System.arraycopy(boards[0], 0, tempBoards[0], 0, 18);
        System.arraycopy(boards[1], 0, tempBoards[1], 0, 18);


        // TODO: Refactor these by precalculating masks without the reliance on mask constants
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

        // These don't change, but are rather kept separate as default initial values for reference
        defaultKingSideCastleMasks[0]  = whiteKingSideMasks;
        defaultKingSideCastleMasks[1]  = blackKingSideMasks;
        defaultQueenSideCastleMasks[0] = whiteQueenSideMasks;
        defaultQueenSideCastleMasks[1] = blackQueenSideMasks;


        // Game Boards with all pieces initial values
        gameBoard  = 0xFFFF00000000FFFFL;
        whiteBoard = 0x000000000000FFFFL;
        blackBoard = 0xFFFF000000000000L;
    }

    /**
     * Moves a Piece from an initial position to a final position.
     *
     * @param move
     *      Move to apply to the board
     */
    public void movePiece(Move move) throws IllegalArgumentException {
        int boardIndex = move.getMovedPieceType().ordinal();
        int colorIndex = move.getMovedPieceColor().ordinal();
        int capture = move.getCapturedPieceType().ordinal();
        long from = move.getFromMask();
        long to = move.getToMask();
        enPassantBoard = 0; // reset enPassantBoard back to default


        if (move.getCastledRook() != Move.CastleSide.NONE) {
            castlePiece(move);
        }
        else {

            if (capture != 6) {
                tempBoards[1 - colorIndex][capture] ^= to; // update temp board to removed captured piece

                // Append the temp board change to the history table, allowing a backtrack mechanism to undo said change
                changeHist[changeHistSize++] = 1 - colorIndex;
                changeHist[changeHistSize++] = capture;
            }
            else if (move.getMovedPieceType() == Piece.Type.PAWN) {
                if (move.getPromotedType() != Piece.Type.NONE) {
                    // if pawn was promoted
                    updatePawnPromotion(move);
                }
                else {
                    // otherwise, check if pawn move changed possible en passant captures
                    checkEnPassantBoard(colorIndex, from, to);
                }
            }
            else if (move.getMovedPieceType() == Piece.Type.KING) {
                // indicate that the king loses its castling privileges once it moves past its starting position.
                tempBoards[colorIndex][6] = 0;
                // update the change history stack
                changeHist[changeHistSize++] = colorIndex;
                changeHist[changeHistSize++] = 6;

                // update the masks for possible king check positions
                updateKingMasks(move);
            }
            else if (move.getMovedPieceType() == Piece.Type.ROOK && boards[colorIndex][6] != 0) {
                // Indicate that a rook looses its castling privileges once it moves past its starting position.
                tempBoards[colorIndex][6] ^= from;

                // add change to the history stack
                changeHist[changeHistSize++] = colorIndex;
                changeHist[changeHistSize++] = 6;
            }





            // Append the original move to the tempBoard
            tempBoards[colorIndex][boardIndex] ^= from | to;
            // update the change history stack
            changeHist[changeHistSize++] = colorIndex;
            changeHist[changeHistSize++] = boardIndex;
        }

        // Finally, verify that the move that was applied did not put our king in check
        if (kingMaskCheck(colorIndex)) {
            // if it did, undo the move that was made
            discardChanges();
            throw new IllegalArgumentException(); // throw a caught exception to be handled by the caller, indicating
                                                  // this move is not possible for the current board configuration.
        }

        // if the move is legal, apply the final changes from tempBoards to boards
        applyChanges();
    }


    /**
     * This will revert the "tempBoards" back to the state that matches "boards" using only the changes referenced by
     * the change history stack "changeHist". This helps to reduce cost by only copying back modified elements, instead
     * of the entire array.
     * <br><br>
     * In addition, we don't actually need to remove these elements from changeHist, since the stack structure
     * facilitates self-management of its elements.
     */
    private void discardChanges() {
        while (changeHistSize > 0) {
            int colorIndex = changeHist[--changeHistSize];
            int boardIndex = changeHist[--changeHistSize];
            tempBoards[colorIndex][boardIndex] = boards[colorIndex][boardIndex];
        }
    }

    /**
     * This is the inverse of undoChanges, applying changes to "boards" instead of "tempBoards". It also will
     * append changes to "gameBoard" which holds the board corresponding to every currently occupied piece in the game.
     */
    private void applyChanges() {
        while (changeHistSize > 0) {
            int colorIndex = changeHist[--changeHistSize];
            int boardIndex = changeHist[--changeHistSize];
            boards[colorIndex][boardIndex] = tempBoards[colorIndex][boardIndex];
            if (boardIndex < 6) {
                // need to update gameBoard as well if a piece move was made (piece masks => boardIndex < 6 )
                gameBoard ^= boards[colorIndex][boardIndex];

                if (colorIndex == 0) {
                    whiteBoard ^= boards[colorIndex][boardIndex];
                }
                else {
                    blackBoard ^= boards[colorIndex][boardIndex];
                }
            }
        }
    }


    /**
     * Castles the king in the king side or queen side configuration as indicated by the Move input. This will also
     * update the king masking bits to their precomputed default values. These changes are made to the temporary boards
     * array, with the history stack updated, and will be verified for legality by movePiece.
     *
     * @param move
     *      Reference move that was castled (move.getCastledRook() != NONE)
     */
    private void castlePiece(Move move) {
        if (move.getMovedPieceColor() == Piece.Color.WHITE) {
            tempBoards[0][6] = 0; // indicate that white can no longer castle
            if (move.getCastledRook() == Move.CastleSide.QUEEN_SIDE) {
                // Castled Queen side
                tempBoards[0][3] ^= 0x01; // remove original rook
                tempBoards[0][3] |= 0x08; // add the castled rooks new location
                tempBoards[0][5]  = 0x04; // add the king's location
            }
            else {
                // Castled King side
                tempBoards[0][3] ^= 0x80; // remove original rook
                tempBoards[0][3] |= 0x20; // add the castled rooks new location
                tempBoards[0][5]  = 0x40; // add the king's location
            }
        }
        else {
            tempBoards[1][6] = 0; // indicate that black can no longer castle
            if (move.getCastledRook() == Move.CastleSide.QUEEN_SIDE) {
                // Castled Queen side
                tempBoards[1][3] ^= 0x0100000000000000L; // remove original rook
                tempBoards[1][3] |= 0x0800000000000000L; // add the castled rooks new location
                tempBoards[1][5]  = 0x0400000000000000L; // add the king's location
            }
            else {
                // Castled King side
                tempBoards[0][3] ^= 0x8000000000000000L; // remove original rook
                tempBoards[0][3] |= 0x2000000000000000L; // add the castled rooks new location
                tempBoards[0][5]  = 0x4000000000000000L; // add the king's location
            }
        }
        int colorIndex = move.getMovedPieceColor().ordinal();

        // Update the changes to history stack accordingly
        changeHist[changeHistSize++] = colorIndex;
        changeHist[changeHistSize++] = 3; // changed the rook mask
        changeHist[changeHistSize++] = colorIndex;
        changeHist[changeHistSize++] = 5; // changed the king mask
        changeHist[changeHistSize++] = colorIndex;
        changeHist[changeHistSize++] = 6; // changed the castle check mask

        updateKingMasksCastle(move);
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

            if ((whiteBoard & mask) != 0) {
                color = Piece.Color.WHITE;
                pieceBitboards = boards[0];
            }
            else {
                color = Piece.Color.BLACK;
                pieceBitboards = boards[1];
            }

            for (int i = 0; i < 6; i++) {
                if ((pieceBitboards[i] & mask) != 0) {
                    return new Piece(pieceTypeArr[i], color); // piece type same as bitboard index
                }
            }

            throw new IllegalStateException(); // thrown in any impossible states that would otherwise be ignored by a
                                               // null return.
        }

        return null; // no piece at that position
    }

    private void updatePawnPromotion(Move move) {
        int promotedType = move.getPromotedType().ordinal();
        int colorIndex = move.getMovedPieceColor().ordinal();
        Piece.Type capturedPiece = move.getCapturedPieceType();
        long from = move.getFromMask();
        long to = move.getToMask();

        if (capturedPiece != Piece.Type.NONE) {
            // captured piece and promoted pawn
            tempBoards[1 - colorIndex][capturedPiece.ordinal()] ^= to; // remove the captured piece

            // Update the history stack to indicate changes made to tempBoards
            changeHist[changeHistSize++] = 1 - colorIndex;
            changeHist[changeHistSize++] = capturedPiece.ordinal();
        }

        tempBoards[colorIndex][0] ^= from; // remove the pawn
        tempBoards[colorIndex][promotedType] |= to; // add the promoted piece

        // Update the history stack to indicate changes made to tempBoards
        changeHist[changeHistSize++] = colorIndex;
        changeHist[changeHistSize++] = 0;
        changeHist[changeHistSize++] = colorIndex;
        changeHist[changeHistSize++] = promotedType;
    }



    private void checkEnPassantBoard(int colorIndex, long from, long to) {
        if ((from & boards[colorIndex][7]) != 0) { // boards[i][7] => enPassantFromMask
            if ((to & boards[colorIndex][8]) != 0) { // boards[i][8] => enPassantToMask
                long mask;
                if ((to & alternatingByteMask) != 0) {
                    mask = alternatingByteMask;
                }
                else { mask = ~alternatingByteMask; }

                if (((to << 1) & mask) != 0) {
                    enPassantBoard |= (to << 1) & boards[1 - colorIndex][0];
                }

                if (((to >>> 1 ) & mask) != 0) {
                    enPassantBoard |= (to >>> 1) & boards[1 - colorIndex][0];
                }
                enPassantBoard |= to;
            }
        }
    }

    /**
     * Returns the boards array corresponding to a colors ordinal value. Note, this allows for unapproved modifications
     * meaning its value shouldn't leave scope unless intended.
     *
     * @param color
     *      Color corresponding to the board of interest
     * @return boards[color.ordinal()]
     */
    protected long[] getColorBoards(Piece.Color color) {
        return boards[color.ordinal()];
    }

    /**
     * Returns the boards array. As with above, this allows for unapproved modifications to the board that could lead to
     * undefined behaviors.
     *
     * @return boards
     */
    protected long[][] getBoards() {
        return boards;
    }

    public long getGameBoard() {
        return gameBoard;
    }

    /**
     * Returns the full board as one long value corresponding to the color input.
     *
     * @param color
     *      Color to find the board of
     * @return long board for the input color
     */
    public long getBoardColor(Piece.Color color) {
        return getBoardColorIndex(color.ordinal());
    }

    /**
     * Returns the full board corresponding to the colorIndex.
     *
     * @param colorIndex
     *      Index of the color to find the board of.
     * @return long board for the input color index
     */
    private long getBoardColorIndex(int colorIndex) {
        return colorIndex == 0 ? whiteBoard : blackBoard;
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
        long kUp     = tempBoards[colorIndex][9];
        long kDown   = tempBoards[colorIndex][10];
        long kLeft   = tempBoards[colorIndex][11];
        long kRight  = tempBoards[colorIndex][12];

        long diagUL  = tempBoards[colorIndex][13];
        long diagUR  = tempBoards[colorIndex][14];
        long diagLL  = tempBoards[colorIndex][15];
        long diagLR  = tempBoards[colorIndex][16];
        long knights = tempBoards[colorIndex][17];

        long kUpPiece = (kUp & gameBoard) & -(kUp & gameBoard);
        if ((colorBoard & kUpPiece) == 0 && (oppColorBoard & kUpPiece) != 0) {
            // See if king is vulnerable above and check to see if an opposing piece of threat is above

            // Only candidates for check are enemy Queen or Rook
            return (kUpPiece & tempBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (kUpPiece & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen

        }

        long kDownPieces = kDown & gameBoard;
        if ((colorBoard & kDownPieces) < (oppColorBoard & kDownPieces)) {
            // if our pieces are less than the opp pieces (for kDown), the opp piece is higher up than ours.

            // We know opposing piece is below, but need to check which one is of direct threat along the attack vector.
            long kingPos = tempBoards[colorIndex][5];
            long pieceOfThreat = getMSB(oppColorBoard & kDownPieces, kingPos, 8);

            // Only candidates of threat are Rook or Queen
            return (pieceOfThreat & tempBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (pieceOfThreat & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long kLeftPieces = kLeft & gameBoard;
        if ((colorBoard & kLeftPieces) < (oppColorBoard & kLeftPieces)) {
            // opponent has a piece in front of our own along the attack vector.

            // find the position of the piece of direct threat to the king
            long kingPos = tempBoards[colorIndex][5];
            long pieceOfThreat = getMSB(kLeftPieces, kingPos, 1);

            // Only candidates of threat are Rook or Queen
            return (pieceOfThreat & tempBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (pieceOfThreat & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long kRightPiece = (kRight & gameBoard) & -(kRight & gameBoard);
        if ((colorBoard & kRightPiece) == 0 && (oppColorBoard & kRightPiece) != 0) {
            // King is vulnerable to the right, and an enemy piece is of direct threat

            // Only candidates of threat are Rook or Queen
            return (kRightPiece & tempBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (kRightPiece & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }


        long diagULPiece = (diagUL & gameBoard) & -(diagUL & gameBoard);
        if ((colorBoard & diagULPiece) == 0) {
            // King is compromised on the upper left diagonal

            // Check to see if opposing piece on diagonal
            if ((oppColorBoard & diagULPiece) != 0) {
                // See if opposing color piece has attack on King

                // Can be a pawn if diagULPiece == kingPos >> 7 (for white or black)
                if (diagULPiece == (tempBoards[colorIndex][5] >> 7)) {
                    return (diagULPiece & tempBoards[1 - colorIndex][0]) != 0 // piece is a pawn
                        || (diagULPiece & tempBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                        || (diagULPiece & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
                }

                // Can be a Bishop or Queen in any compromised square
                return (diagULPiece & tempBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                    || (diagULPiece & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen

            }
        }

        long diagURPiece = (diagUR & gameBoard) & -(diagUR & gameBoard);
        if ((colorBoard & diagURPiece) == 0 && (oppColorBoard & diagURPiece) != 0) {
            // King is compromised on the upper right diagonal and opposing piece on diagonal

            // Can be a pawn if diagULPiece == kingPos >> 9 (for white or black)
            if (diagURPiece == (tempBoards[colorIndex][5] >> 9)) {
                return (diagURPiece & tempBoards[1 - colorIndex][0]) != 0 // piece is a pawn
                    || (diagURPiece & tempBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                    || (diagURPiece & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
            }

            // Can be a Bishop or Queen in any other compromised square
            return (diagURPiece & tempBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                || (diagURPiece & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long diagLLPieces = diagLL & gameBoard;
        if ((colorBoard & diagLLPieces) < (oppColorBoard & diagLLPieces)) {
            // King is exposed on the lower left diagonal.

            long kingPos = tempBoards[colorIndex][5];
            long pieceOfThreat = getMSB(oppColorBoard & diagLLPieces, kingPos, 9);

            // King only in check to enemy bishop or queen on the attack vector
            return (pieceOfThreat & tempBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                || (pieceOfThreat & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long diagLRPieces = diagLR & gameBoard;
        if ((colorBoard & diagLRPieces) < (oppColorBoard & diagLRPieces)) {
            // King is exposed on the lower right diagonal.

            long kingPos = tempBoards[colorIndex][5];
            long pieceOfThreat = getMSB(oppColorBoard & diagLRPieces, kingPos, 7);

            // King only in check to enemy bishop or queen along the attack vector
            return (pieceOfThreat & tempBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                || (pieceOfThreat & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        // Check to see if any of the opponents knights have a check position on the king.
        return (tempBoards[1 - colorIndex][1] & knights) != 0;
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
        int colorIndex = move.getMovedPieceColor().ordinal();
        long oldPos = move.getFromMask();
        long newPos = move.getToMask();

        long rightBoundXor = 0x0101010101010101L;  // illegal col 0 for diagLR and diagUR
        long leftBoundXor  = 0x8080808080808080L;  // illegal col 7 for diagLL and diagUL

        KingMoveDir moveDirection = findKingDirection(oldPos, newPos);

        long[] kingMask = tempBoards[colorIndex];

        // local variables to aid in readability
        long kUp     = kingMask[9]; // mask UP
        long kDown   = kingMask[10]; // mask DOWN
        long kLeft   = kingMask[11]; // mask LEFT
        long kRight  = kingMask[12]; // mask RIGHT

        long diagUL  = kingMask[13]; // mask UPPER LEFT
        long diagUR  = kingMask[14]; // mask UPPER RIGHT
        long diagLL  = kingMask[15]; // mask LOWER LEFT
        long diagLR  = kingMask[16]; // mask LOWER RIGHT

        long knights = kingMask[8]; // mask KNIGHT SQUARES

        if (moveDirection == KingMoveDir.UP) {

            kingMask[9]  = oldPos | kUp;
            kingMask[10] = newPos ^ kDown;

            kingMask[11] = kLeft  << 8;
            kingMask[12] = kRight << 8;

            kingMask[13] = diagUL << 8;
            kingMask[14] = diagUR << 8;

            kingMask[15] = (oldPos >>> 1) | (diagLL << 8);
            kingMask[16] = (oldPos << 1)  | (diagLR << 8);

            kingMask[17] = (knights << 8) | (oldPos << 2) | (oldPos >>> 2) | (oldPos >>> 7) | (oldPos >>> 9);

        }
        else if (moveDirection == KingMoveDir.DOWN) {

            kingMask[9]  = newPos ^ kUp;
            kingMask[10] = oldPos | kDown;

            kingMask[11] = kLeft  >>> 8;
            kingMask[12] = kRight >>> 8;

            kingMask[13] = (oldPos >>> 1) | (diagUL >>> 8);
            kingMask[14] = (oldPos << 1)  | (diagUR >>> 8);

            kingMask[15] = diagLL >>> 8;
            kingMask[16] = diagLR >>> 8;

            kingMask[17] = (knights >>> 8) | (oldPos << 2) | (oldPos >>> 2) | (oldPos << 7) | (oldPos << 9);

        }
        else if (moveDirection == KingMoveDir.LEFT) {

            kingMask[9]  = kUp   >>> 1;
            kingMask[10] = kDown >>> 1;

            kingMask[11] = kLeft  ^ newPos;
            kingMask[12] = kRight | oldPos;

            kingMask[13] = (diagUL >>> 1) ^ leftBoundXor;
            kingMask[14] = (oldPos | diagUR) << 8;

            kingMask[15] = (diagLL >>> 1) ^ leftBoundXor;
            kingMask[16] = (oldPos | diagLR) >>> 8;

            kingMask[17] = (knights >>> 1) ^ leftBoundXor;

        }
        else if (moveDirection == KingMoveDir.RIGHT) {

            kingMask[9]  = kUp   << 1;
            kingMask[10] = kDown << 1;

            kingMask[11] = kLeft | oldPos;
            kingMask[12] = kRight ^ newPos;

            kingMask[13] = (oldPos | diagUL) << 8;
            kingMask[14] = (diagUR << 1) ^ rightBoundXor;

            kingMask[15] = (oldPos | diagLL) >>> 8;
            kingMask[16] = (diagLR << 1) ^ rightBoundXor;

            kingMask[17] = (knights << 8) ^ rightBoundXor;
        }
        else if (moveDirection == KingMoveDir.UPPER_LEFT) {

            kingMask[9]  = (kUp >>> 1) ^ newPos;
            kingMask[10] = (kDown | oldPos) >>> 1;

            kingMask[11] = (kLeft << 8) ^ newPos;
            kingMask[12] = (kRight | oldPos) << 8;

            kingMask[13] = diagUL ^ newPos;
            kingMask[14] = (oldPos | diagUR) << 16;

            kingMask[15] = ((oldPos | diagLL) >>> 2) ^ (leftBoundXor | (leftBoundXor >>> 1));
            kingMask[16] = diagLR | oldPos;

            kingMask[17] = (((knights << 7) | (oldPos >>> 3) | (oldPos >>> 10)) ^ (leftBoundXor | (leftBoundXor >>> 1)))
                    | (oldPos << 24)  | (oldPos >>> 8)
                    | (((oldPos << 1) | (oldPos << 17)) ^ rightBoundXor) ;

        }
        else if (moveDirection == KingMoveDir.UPPER_RIGHT) {

            kingMask[9]  = (kUp << 1) ^ newPos;
            kingMask[10] = (kDown | oldPos) << 1;

            kingMask[11] = (kLeft | oldPos) << 8;
            kingMask[12] = (kRight << 8) ^ newPos;

            kingMask[13] = (oldPos | diagUL) << 16;
            kingMask[14] = diagUR ^ newPos;

            kingMask[15] = diagLL | oldPos;
            kingMask[16] = ((oldPos | diagLR) << 2) ^ (rightBoundXor | (rightBoundXor << 1));

            kingMask[17] = (((oldPos >>> 1) | (oldPos << 15)) ^ leftBoundXor)
                    | (oldPos >>> 8) | (oldPos << 24)
                    | (((oldPos << 3) | (knights << 9) | (oldPos >>> 6)) ^ (rightBoundXor | (rightBoundXor << 1)));

        }
        else if (moveDirection == KingMoveDir.LOWER_LEFT) {

            kingMask[9]  = (kUp | oldPos) >>> 1;
            kingMask[10] = (kDown >>> 1) ^ newPos;

            kingMask[11] = (kLeft >>> 8) ^ newPos;
            kingMask[12] = (kRight | oldPos) >>> 8;

            kingMask[13] = ((oldPos | diagUL) >>> 2) ^ (leftBoundXor | (leftBoundXor >>> 1));
            kingMask[14] = oldPos | diagUR;

            kingMask[15] = diagLL ^ newPos;
            kingMask[16] = (oldPos | diagLR) >>> 16;

            kingMask[17] = (((knights >>> 9) | (oldPos >>> 3) | (oldPos << 6)) ^ (leftBoundXor | (leftBoundXor >>> 1)))
                    | (oldPos >>> 24) | (oldPos << 8)
                    | (((oldPos << 1) | (oldPos >>> 15)) ^ rightBoundXor);

        }
        else {// moveDirection == KingMoveDir.LOWER_RIGHT

            kingMask[9]  = (kUp | oldPos) << 1;
            kingMask[10] = (kDown << 1) ^ newPos;

            kingMask[11] = (kLeft | oldPos) >>> 8;
            kingMask[12] = (kRight >>> 8) ^ newPos;

            kingMask[13] = ((oldPos | diagUR) << 2) ^ (rightBoundXor | (rightBoundXor << 1));
            kingMask[14] = diagUL | oldPos;

            kingMask[15] = diagLR ^ newPos;
            kingMask[16] = (oldPos | diagLL) >>> 16;

            kingMask[17] = (((oldPos >>> 1) | (oldPos >>> 17))  ^ leftBoundXor)
                    | (oldPos << 8) | (oldPos >>> 24)
                    | (((knights >>> 7) | (oldPos << 3) | (oldPos << 10)) ^ (rightBoundXor | rightBoundXor << 1));
        }

        // Make sure to update these changes to the change history stack
        for (int i = 9; i <= 17; i++) {
            changeHist[changeHistSize++] = colorIndex;
            changeHist[changeHistSize++] = i;
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
        if (move.getCastledRook() == Move.CastleSide.NONE) {
            throw new IllegalStateException("Passed a non-castled move to the update castle mask method!");
        }
        int colorIndex = move.getMovedPieceColor().ordinal();
        if (move.getCastledRook() == Move.CastleSide.QUEEN_SIDE) {
            System.arraycopy(defaultQueenSideCastleMasks[colorIndex], 0, tempBoards[colorIndex], 9, 9);
        }
        else { // move.getCastledRook() == Move.CastleSide.KING_SIDE
            System.arraycopy(defaultKingSideCastleMasks[colorIndex], 0, tempBoards[colorIndex], 9, 9);
        }

        // Update the change history stack
        for (int i = 9; i <= 17; i++) {
            changeHist[changeHistSize++] = colorIndex;
            changeHist[changeHistSize++] = i;
        }
    }
}