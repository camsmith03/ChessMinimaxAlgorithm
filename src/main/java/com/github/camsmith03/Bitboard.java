package com.github.camsmith03;

/**
 * <p>
 * Bitboard implemented using longs that represent a 64 square chess
 * board (1 for occupied, 0 for empty). Highly more efficient than other 
 * approaches as far less space is consumed by the game board. This also adds an
 * additional feature of move virtualization, which will allow paths to the
 * traversed, then returned back to the start without the need to worry about
 * loosing the previous state. As of now, it allows for single path traversal,
 * however, plans are made to reimplement the SaveState class as a circular
 * based stack with the size of ply. This would remove the need for hard copies
 * to be made, allowing the algorithm to truly leverage the power bitboards have
 * to offer over conventional implementations.
 * </p>
 * <p>
 * All the methods and implementations were created by myself. Any constants
 * (including starting masks and mask manipulations) were painstakingly found
 * using hand computations on my iPad notes app. While these are likely freely
 * available, the manual derivation helped to significantly improve my hex and
 * binary conversion skills, as well as my general understanding as to how the
 * boards work.
 * </p>
 * <p>
 * Follows standard Bitboard conventions:
 * <ul><li>LSB: Square a1</li>
 *     <li>MSB: Square h8</li>
 * </ul>
 * </p>
 *
 * @author Cameron Smith
 * @version 07.26.2024
 */
public class Bitboard {
    private final Piece.Type[] pieceTypeArr = Piece.Type.values();
    private static final long alternatingByteMask = 0xFF00FF00FF00FF00L;

    /*  WHITE INITIAL KING MASK CONSTANTS
    These bits are the precomputed masks that serve primarily as reference and
    allow for future modifications if need be. They are based on the direction
    for the squares surrounding the white king.
    */
    private static final long whiteKingUpMask     = 0x1010101010101000L;
    private static final long whiteKingDownMask   = 0;
    private static final long whiteKingLeftMask   = 0x000000000000000FL;
    private static final long whiteKingRightMask  = 0x00000000000000E0L;
    private static final long whiteKingDiagUL     = 0x0000000102040800L;
    private static final long whiteKingDiagUR     = 0x0000000080402000L;
    private static final long whiteKingDiagLL     = 0;
    private static final long whiteKingDiagLR     = 0;
    private static final long whiteKingKnightMask = 0x0000000000284400L;

    /* BLACK INITIAL KING MASK CONSTANTS */
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

    private enum KingMoveDir { UP, DOWN, LEFT, RIGHT, UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT }

    public long enPassantBoard = 0; // Flag that stays zero unless a move from either side could allow for an ep capture
                                    // Its value is un-flipped after the next turn (in line with ep rules).


    // Board backing storage fields
    private final long[][] boards = new long[2][];
    private long gameBoard;
    private long whiteBoard;
    private long blackBoard;

    // Storage allocated to undo any moves that lead to positions that place the king in check
    private final long[][] tempBoards = new long[2][]; // Copy of boards to allow for virtual nondestructive, movements.
    private final long[] tempColorBoards;
    private int[] changeHist = new int[30]; // Acts as an array of 15 pairs of integers (int modifiedColorIndex, int
                                            // modifiedMaskIndex).
    private int changeHistSize; // essentially the stack pointer to modified boards


    /* === MOVE VIRTUALIZATION FLAGS === */

    /*
    the virtualState flag is used to apply virtual moves to the board to test
    move legality without write back to the boards array.
     */
    private boolean virtualState = false; //

    /*
    the virtualCall flag is flipped to true when virtualMove() is invoked,
    allowing the subsequent call to movePiece() to maintain current state.
    Otherwise, it's assumed movePiece() call has intent to modify saved state.
     */
    private boolean virtualCall = false; //

    /*
    the virtualLock lock is used to allow the user to keep their instance of
    virtualization in the event they hit an illegal position. While they can't
    revert back, they may want access to data prior to wiping the instance.
     */
    private boolean virtualLock = false;

    /**
     * Bitboard's constructor will set the default values for each of its
     * fields, assuming the expected default is a normal starting chess game.
     * <br>
     * The "boards" long array acts as the mask storage that will hold all the
     * masks corresponding to the current game. These mask values have been
     * precomputed by myself to speed up needless bit operations. There are two
     * boards, and the color being stored depends on the color's ordinal value
     * as the starting index.
     *     > Ex: boards[ordinalColorIndex][maskIndex]
     *     // ordinalColorIndex=0 -> white, =1 -> black
     * <p>
     * The PIECE TYPE MASKS:     These correspond to indices of the ordinal
     *                           values for the piece types. usage must adhere
     *                           to the fact that Piece.Type.NONE exists and has
     *                           an ordinal value of 6.
     * </p><p>
     * The CASTLING ROOKS MASKS: This holds the mask that is defaulted to the
     *                           location of the two rooks. When a side looses
     *                           its ability to castle (on either king side or
     *                           queen side) the rook used during the move has
     *                           its bit set to zero. This would occur if either
     *                           rook was moved. Also, if the king is moved at
     *                           any point, this is set to zero to indicate that
     *                           castling is no longer available.
     * </p><p>
     * The KING CHECK MASKS:     These are the masking bits corresponding to the
     *                           squares that *could* pose a threat to the king.
     *                           They are essential in ensuring no moves were
     *                           attempted that would put the moved piece's king
     *                           in check. They are changed only when the king
     *                           is moved for either side.
     * </p>
     *
     */
    public Bitboard() {
        // Initial white board masking bits
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
                0x00FF000000000000L, // En Passant From Location Masks
                0x000000FF00000000L, // En Passant To Location Masks

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

        // Initial black board masking bits
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
        tempBoards[0] = new long[18];
        tempBoards[1] = new long[18];
        System.arraycopy(boards[0], 0, tempBoards[0], 0, 18);
        System.arraycopy(boards[1], 0, tempBoards[1], 0, 18);


        // TODO: Refactor these by precalculating masks without the reliance on mask constants
        long[] whiteKingSideMasks = new long[]{
                whiteKingUpMask    << 2,
                0, // kDown
                whiteKingLeftMask  |  0x30,
                whiteKingRightMask ^  0x60,
                0x0001020408102000L, // diagUL
                0x0000000000008000L, // diagUR
                0, // diagLL
                0, // diagLR
                0x000000000A01000L // knights
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


        // GameEngine Boards with all pieces initial values
        gameBoard  = 0xFFFF00000000FFFFL;
        whiteBoard = 0x000000000000FFFFL;
        blackBoard = 0xFFFF000000000000L;
        tempColorBoards = new long[]{0x000000000000FFFFL, 0xFFFF000000000000L};
    }

    /**
     * Moves a Piece from an initial position to a final position.
     *
     * @param move
     *      Move to apply to the board
     */
    public void movePiece(Move move) throws IllegalArgumentException {
        if (!virtualCall && virtualState) {
            // movePiece() hasn't been called by virtualMove(), but history of tempBoards has deviated due to an active
            // virtual state. For safety, we assume the call has no association to the virtual instance, and wipe out
            // the current tempBoards state.
            wipeVirtualization();
        }
        virtualCall = false; // set virtualization flag to false for future calls to movePiece()

        int boardIndex = move.getMovedPieceType().ordinal();
        int colorIndex = move.getMovedPieceColor().ordinal();
        long enPassantBit = move.getEnPassant();
        long from = move.getFromMask();
        long to = move.getToMask();
        enPassantBoard = 0; // reset enPassantBoard back to default


        if (move.getCastledRook() != Move.CastleSide.NONE)
            castlePiece(move);
        else {
            tempColorBoards[colorIndex] ^= to | from;
            int capture = move.getCapturedPieceType().ordinal();

            if (capture != 6 && enPassantBit == 0) {
                tempBoards[1 - colorIndex][capture] ^= to; // update temp board to removed captured piece
                tempColorBoards[1 - colorIndex] ^= to;

                // Append the temp board change to the history table, allowing a backtrack mechanism to undo said change
                appendChange(1 - colorIndex, capture);
            }

            if (move.getMovedPieceType() == Piece.Type.PAWN) {
                if (move.getPromotedType() != Piece.Type.NONE) // if pawn was promoted
                    updatePawnPromotion(move);
                else if (enPassantBit != 0) {
                    // an en passant capture is made. update with the saved location of the captured pawn
                    tempBoards[1 - colorIndex][0] ^= enPassantBit;
                    tempColorBoards[1 - colorIndex] ^= enPassantBit;
                    appendChange(1 - colorIndex, 0);
                }
                else // otherwise, check if pawn move changed possible en passant captures
                    checkEnPassantBoard(colorIndex, from, to);
            }
            else if (move.getMovedPieceType() == Piece.Type.KING) {
                // indicate that the king loses its castling privileges once it moves past its starting position.
                tempBoards[colorIndex][6] = 0;

                // update the change history stack
                appendChange(colorIndex, 6);

                // update the masks for possible king check positions
                updateKingMasks(move);
            }
            else if (move.getMovedPieceType() == Piece.Type.ROOK && tempBoards[colorIndex][6] != 0) {
                // Indicate that a rook looses its castling privileges once it moves past its starting position.
                tempBoards[colorIndex][6] ^= from;
                // add change to the history stack
                appendChange(colorIndex, 6);
            }

            // Append the original move to the tempBoard
            tempBoards[colorIndex][boardIndex] ^= from | to;
            // update the change history stack
            appendChange(colorIndex, boardIndex);
        }

        // Finally, verify that the move that was applied did not put our king in check
        if (kingMaskCheck(colorIndex)) {
            // if it did, undo the move that was made, unless the instance is a virtual one.
            if (virtualState) {
                // if it is, apply the virtualLock to keep the current virtual instance, but signify no further changes
                // can be applied, and the instance is strictly read-only until it is wiped.
                virtualLock = true;
                throw new IllegalArgumentException("Virtual instance encountered a move that put king in check (virtual lock now enabled)");
            }
            discardChanges();
            // throw a caught exception to be handled by the caller, indicating this move is not possible for the
            // current board configuration.
            throw new IllegalArgumentException("Attempted move illegally placed the king in check (reverted back).");
        }

        // if the move is legal, apply the final changes from tempBoards to boards only if the virtualState flag is set
        // to false. This allows for move virtualization that can be used to apply temporary moves without affecting
        // the core boards themselves.
        if (!virtualState)
            applyChanges();
        else // if we are in a virtual state, update our gameBoard so any calls to getPiece use the virtual state boards
            gameBoard = tempColorBoards[colorIndex] | tempColorBoards[1 - colorIndex];

    }

    /**
     * Appends changes to the changeHist array. Will increase it's size if the
     * limit has been reached. Since the value of changeHist is a multiple of
     * two, and every change added requires two inputs, size limits only need to
     * be checked once per insertion.
     *
     * @param colorIndex
     *      Index of the color board changed
     * @param boardIndex
     *      Index of the boards[colorIndex] changed
     */
    private void appendChange(int colorIndex, int boardIndex) {
        if (changeHistSize == changeHist.length)  growChangeHist();

        changeHist[changeHistSize++] = colorIndex;
        changeHist[changeHistSize++] = boardIndex;
    }

    /**
     * Grow the size of the changeHist array. Unless virtualization explores
     * deep into future moves, the storage size is unlikely to require growth.
     * Nonetheless, if the limit is reached, the size of the array will be
     * doubled.
     */
    private void growChangeHist() {
        int[] newChangeHist = new int[changeHistSize * 2];
        System.arraycopy(changeHist, 0, newChangeHist, 0, changeHistSize);
        changeHist = newChangeHist;
    }

    /**
     * This will revert the "tempBoards" back to the state that matches "boards"
     * using only the changes referenced by the change history stack
     * "changeHist". This helps to reduce cost by only copying back modified
     * elements, instead of the entire array.
     * <br><br>
     * In addition, we don't actually need to remove these elements from
     * changeHist, since the stack structure facilitates self-management of its
     * elements.
     */
    private void discardChanges() {
        while (changeHistSize > 0) {
            int boardIndex = changeHist[--changeHistSize];
            int colorIndex = changeHist[--changeHistSize];
            tempBoards[colorIndex][boardIndex] = boards[colorIndex][boardIndex];
        }
        tempColorBoards[0] = whiteBoard;
        tempColorBoards[1] = blackBoard;
    }

    /**
     * This is the inverse of undoChanges, applying changes to "boards" instead
     * of "tempBoards". It also will append changes to "gameBoard" which holds
     * the board corresponding to every currently occupied piece in
     * the game.
     */
    private void applyChanges() {
        while (changeHistSize > 0) {
            int boardIndex = changeHist[--changeHistSize];
            int colorIndex = changeHist[--changeHistSize];
            boards[colorIndex][boardIndex] = tempBoards[colorIndex][boardIndex];
        }
        whiteBoard = tempColorBoards[0];
        blackBoard = tempColorBoards[1];
        gameBoard = whiteBoard | blackBoard;
    }

    /**
     * <p>
     * Virtual move offers the powerful ability to modify the bitboard, yet
     * maintain the state of boards to fall back on after the move was made.
     * Modifications are written to tempBoards, with changeHist updates, but
     * nothing gets written permanently.
     * </p><p>
     * Utilizes a set two flags and one lock.
     * <ul>
     * <li><b>virtualState</b> is flipped to true on the first time the virtual
     * instance is used to indicate the current board is in a virtual usage
     * state. It will only get flipped back once the instance has ended.</li>
     *
     * <li><b>virtualCall</b> gets flipped once every time virtualMovePiece() is
     * called. This grants communication between movePiece() and
     * virtualMovePiece(), allowing the former to know not to wipe the virtual
     * instance if it was properly invoked with the flag being set to true. The
     * flag is set to false immediately afterward for the next invocation.</li>
     *
     * <li><b>virtualLock</b> is a one-way flag that indicates a illegal board
     * state has been reached on a given virtual instance, and no further moves
     * can be made. This pauses any write back procedures to allow data to be
     * collected that is relevant to the state it's in. In order to make changes
     * again, the state needs to be wiped by either invoking movePiece() for a
     * permanent move on the original board, or calling the
     * wipeVirtualization() method.</li></ul>
     * </p><p>
     * Note: if a state is currently being used for virtualization, any attempts
     * to invoke movePiece() instead of virtualMovePiece() will always wipe the
     * virtualized instance and apply the move to the original board
     * permanently.
     * </p>
     */
    public void virtualMovePiece(Move move) throws IllegalArgumentException {
        virtualState = true;
        virtualCall = true;
        if (!virtualLock) {
            movePiece(move);
        }
        else {
            throw new IllegalArgumentException("Virtual lock is enabled. No further changes can be made for this instance.");
        }
    }

    /**
     * Applies a virtual move to the board, checks for legality, and returns a
     * boolean to indicate true if the move was successful and did not put the
     * king in check. By default, this will not delete the virtual instance in
     * case the caller wants to maintain it, assuming the instance is legal.
     *
     * @param move
     *      Move to test on the board for check legality.
     * @return true if the move didn't put the king in check; false otherwise.
     */
    public boolean isMoveLegal(Move move) {
        if (virtualLock || virtualState)
            wipeVirtualization(); // wipe the previous virtual instance if one exists

        virtualState = true;
        virtualCall = true;
        try {
            movePiece(move);
            return true; // move successful and didn't lead to exception. maintain it's state for caller use
        }
        catch (IllegalArgumentException e) {
            wipeVirtualization(); // undo the virtualized move and return a false value to indicate the move was illegal
            return false;
        }
    }

    /**
     * Wipes the virtual instance back to the original board state.
     * This will remove the virtualLock, and will allow for a new virtual
     * instance to be utilized if desired.
     */
    public void wipeVirtualization() {
        virtualLock = false;
        virtualState = false;
        discardChanges();
    }

    /**
     * Writes the changes made by a virtual instance to the original board
     * state. If the instance has a virtual lock enabled, the changes cannot be
     * written.
     *
     * @throws IllegalArgumentException
     *      If the invoking board isn't in a virtual state or the virtual lock
     *      is enabled to prevent write back.
     */
    public void applyVirtualMovePermanently() throws IllegalArgumentException {
        if (!virtualState)
            throw new IllegalArgumentException("Board must be in a virtual state to apply any virtual move permanently.");
        else if (virtualLock)
            throw new IllegalArgumentException("Virtual lock is enabled and the instance cannot be permanently written.");

        virtualState = false;
        applyChanges();
    }

    /**
     * <p>
     * Returns a SaveState object that contains a hard copy for the current
     * Bitboard state. Calls to restoreSaveState with the SaveState provided
     * will return the game to the state it was previously in.
     * </p><p>
     * Note that anytime restoreSaveState is invoked, it will permanently switch
     * the state back, without warning (regardless of whether virtualization is
     * actively in use). Those utilizing this feature must heed
     * caution to ensure they don't lose data that may be relevant.
     * </p>
     *
     * @return SaveState
     */
    public SaveState saveCurrentState() {
        // TODO: refactor to move virtualization with circular queue.
        return new SaveState(tempBoards, tempColorBoards, enPassantBoard);
    }

    /**
     * <p>
     * When given a SaveState object, this will restore the current bitboard to
     * the state at which it was previously in.
     * </p>
     * <p>
     * This call is permanent and irreversible, so those using it should ensure
     * all data in the bitboard about to be wiped is no longer relevant.
     * </p>
     *
     * @param saveState
     *      object from a previous state the board was in.
     */
    public void restoreSaveState(SaveState saveState) {
        // TODO: refactor to move virtualization with circular stack.
        virtualLock = false;
        virtualState = false;

        System.arraycopy(saveState.boards[0], 0, this.boards[0], 0, 18);
        System.arraycopy(saveState.boards[1], 0, this.boards[1], 0, 18);
        System.arraycopy(saveState.colorBoards, 0, tempColorBoards, 0, 2);

        this.enPassantBoard = saveState.epBoard;

        for (int i = 0; i < 18; i++) {
            tempBoards[0][i] = boards[0][i];
            tempBoards[1][i] = boards[1][i];
        }
        whiteBoard = tempColorBoards[0];
        blackBoard = tempColorBoards[1];
        gameBoard = whiteBoard | blackBoard;
        changeHistSize = 0;
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
                tempColorBoards[0] ^= 0x0F; // update the white color board
            }
            else {
                // Castled King side
                tempBoards[0][3] ^= 0x80; // remove original rook
                tempBoards[0][3] |= 0x20; // add the castled rooks new location
                tempBoards[0][5]  = 0x40; // add the king's location
                tempColorBoards[0] ^= 0xF0; // update the white color board
            }
        }
        else {
            tempBoards[1][6] = 0; // indicate that black can no longer castle
            if (move.getCastledRook() == Move.CastleSide.QUEEN_SIDE) {
                // Castled Queen side
                tempBoards[1][3] ^= 0x0100000000000000L; // remove original rook
                tempBoards[1][3] |= 0x0800000000000000L; // add the castled rooks new location
                tempBoards[1][5]  = 0x0400000000000000L; // add the king's location
                tempColorBoards[1] ^= 0x0F00000000000000L; // update the black color board
            }
            else {
                // Castled King side
                tempBoards[0][3] ^= 0x8000000000000000L; // remove original rook
                tempBoards[0][3] |= 0x2000000000000000L; // add the castled rooks new location
                tempBoards[0][5]  = 0x4000000000000000L; // add the king's location
                tempColorBoards[0] ^= 0xF000000000000000L; // update the black color board
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
     * When given a board position, this will return the game piece that
     * corresponds to any piece that may exist at that coordinate, otherwise it
     * will return null.
     *
     * @param mask
     *      bit mask to check for a piece's location of.
     * @return Piece if exists, null otherwise
     */
    public Piece getPiece(long mask) {
        if ((gameBoard & mask) != 0) {
            Piece.Color color;
            long[] pieceBitboards;

            if ((tempColorBoards[0] & mask) != 0) {
                color = Piece.Color.WHITE;
                pieceBitboards = tempBoards[0];
            }
            else {
                color = Piece.Color.BLACK;
                pieceBitboards = tempBoards[1];
            }

            for (int i = 0; i < 6; i++) {
                if ((pieceBitboards[i] & mask) != 0)
                    return new Piece(pieceTypeArr[i], color); // piece type same as bitboard index
            }
            throw new IllegalStateException(); // thrown in any impossible states that would otherwise be ignored by a
                                               // null return.
        }
        return null; // no piece at that position
    }

    /**
     * When a move is marked for pawn promotion, it is passed into this private
     * method so that the modifications get made to the bitboards.
     *
     * @param move
     *      of a pawn to a promoted state.
     */
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

    /**
     * If any pawn was moved to a position that would allow the opposing side to
     * capture said pawn on the next turn (via en passant), this will recognize
     * that and update the public field enPassantBoard. This field is reset to
     * zero after each move, ensuring that the legal form of capture is upheld.
     *
     * @param colorIndex
     *      Index of the color making the pawn move.
     * @param from
     *      masking bit for the pawns original position.
     * @param to
     *      masking bit for the pawns new position.
     */
    private void checkEnPassantBoard(int colorIndex, long from, long to) {
        // TODO: get rid of unmodified enPassant masks in the boards array
        if ((from & tempBoards[1 - colorIndex][7]) != 0) { // boards[i][7] => enPassantFromMask
            if ((to & tempBoards[1 - colorIndex][8]) != 0) { // boards[i][8] => enPassantToMask
                long mask;
                if ((to & alternatingByteMask) != 0)
                    mask = alternatingByteMask;
                else
                    mask = ~alternatingByteMask;

                if (((to << 1) & mask) != 0)
                    enPassantBoard |= (to << 1) & tempBoards[1 - colorIndex][0];

                if (((to >>> 1 ) & mask) != 0)
                    enPassantBoard |= (to >>> 1) & tempBoards[1 - colorIndex][0];

                enPassantBoard |= to;
            }
        }
    }

    /**
     * Returns the boards array corresponding to a colors ordinal value. Note,
     * this allows for unapproved modifications meaning its value shouldn't
     * leave scope unless intended.
     *
     * @param color
     *      corresponds to the board of interest
     * @return tempBoards[color.ordinal()]
     */
    protected long[] getColorBoards(Piece.Color color) {
        return tempBoards[color.ordinal()];
    }

    /**
     * Returns the boards array. As with above, this allows for unapproved
     * modifications to the board that could lead to undefined
     * behaviors.
     *
     * @return boards
     */
    protected long[][] getBoards() {
        return boards;
    }

    /**
     * Returns the virtual boards (i.e.: temp boards). The same as
     * getTempBoards(), but the usage is more clear with the given name.
     *
     * @return tempBoards
     */
    protected long[][] getVirtualBoards() {
        return tempBoards;
    }

    /**
     * Returns the virtual color boards.
     *
     * @return tempColorBoards
     */
    public long[] getVirtualColorBoards() {
        return tempColorBoards;
    }

    /**
     * Returns the game board, which has been updated for the virtual state if
     * it exists.
     *
     * @return gameBoard
     */
    public long getGameBoard() {
        return gameBoard;
    }

    /**
     * Returns the full board as one long value corresponding to the
     * color input.
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
     * When passed a color's ordinal value (i.e.: index), this will determine if
     * that color's king is in a checked position. The masks are initialized to
     * their default values, and only transformed for any king moves during the
     * game.
     *
     * @param colorIndex
     *      Color ordinal value of king to inspect.
     * @return true if king is in check; false otherwise.
     */
    public boolean kingMaskCheck(int colorIndex) {
        long tempGameBoard = tempColorBoards[0] | tempColorBoards[1];
        long colorBoard = tempColorBoards[colorIndex];
        long oppColorBoard = tempColorBoards[1 - colorIndex];
        long kUp     = tempBoards[colorIndex][9];
        long kDown   = tempBoards[colorIndex][10];
        long kLeft   = tempBoards[colorIndex][11];
        long kRight  = tempBoards[colorIndex][12];
        long diagUL  = tempBoards[colorIndex][13];
        long diagUR  = tempBoards[colorIndex][14];
        long diagLL  = tempBoards[colorIndex][15];
        long diagLR  = tempBoards[colorIndex][16];
        long knights = tempBoards[colorIndex][17];

        long kUpPiece = (kUp & tempGameBoard) & -(kUp & tempGameBoard);
        if ((oppColorBoard & kUpPiece) != 0) {
            // See if king is vulnerable above and check to see if an opposing piece of threat is above

            // Only candidates for check are enemy Queen or Rook
            return (kUpPiece & tempBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (kUpPiece & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long kDownPieces = kDown & tempGameBoard;
        if ((colorBoard & kDownPieces) < (oppColorBoard & kDownPieces)) {
            // if our pieces are less than the opp pieces (for kDown), the opp piece is higher up than ours.

            // We know opposing piece is below, but need to check which one is of direct threat along the attack vector.
            long kingPos = tempBoards[colorIndex][5];
            long pieceOfThreat = getMSB(oppColorBoard & kDownPieces, kingPos, 8);

            // Only candidates of threat are Rook or Queen
            return (pieceOfThreat & tempBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (pieceOfThreat & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long kLeftPieces = kLeft & tempGameBoard;
        if ((colorBoard & kLeftPieces) < (oppColorBoard & kLeftPieces)) {
            // opponent has a piece in front of our own along the attack vector.

            // find the position of the piece of direct threat to the king
            long kingPos = tempBoards[colorIndex][5];
            long pieceOfThreat = getMSB(kLeftPieces, kingPos, 1);

            // Only candidates of threat are Rook or Queen
            return (pieceOfThreat & tempBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (pieceOfThreat & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long kRightPiece = (kRight & tempGameBoard) & -(kRight & tempGameBoard);
        if ((oppColorBoard & kRightPiece) != 0) {
            // King is vulnerable to the right, and an enemy piece is of direct threat

            // Only candidates of threat are Rook or Queen
            return (kRightPiece & tempBoards[1 - colorIndex][3]) != 0 // piece is a rook
                || (kRightPiece & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }


        long diagULPiece = (diagUL & tempGameBoard) & -(diagUL & tempGameBoard);
        if ((oppColorBoard & diagULPiece) != 0) {
            // King is compromised on the upper left diagonal

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

        long diagURPiece = (diagUR & tempGameBoard) & -(diagUR & tempGameBoard);
        if ((oppColorBoard & diagURPiece) != 0) {
            // King is compromised on the upper right diagonal

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

        long diagLLPieces = diagLL & tempGameBoard;
        if ((colorBoard & diagLLPieces) < (oppColorBoard & diagLLPieces)) {
            // King is exposed on the lower left diagonal.

            long kingPos = tempBoards[colorIndex][5];
            long pieceOfThreat = getMSB(oppColorBoard & diagLLPieces, kingPos, 9);

            // King only in check to enemy bishop or queen on the attack vector
            return (pieceOfThreat & tempBoards[1 - colorIndex][2]) != 0 // piece is a bishop
                || (pieceOfThreat & tempBoards[1 - colorIndex][4]) != 0;// piece is a queen
        }

        long diagLRPieces = diagLR & tempGameBoard;
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
     * Acquires the MSB from a mask relating to a kings potential vulnerable
     * squares. The step size relates to the direction of the kingMask, from the
     * king's starting square. The MSB for the mask is associated with the first
     * piece closest to the king along the direction of vulnerability.
     * </p><p>
     * Note, usage requires a kingMask != 0. In other words, there must be a 1
     * following in the direction of the step size, otherwise an exception will
     * be thrown.
     * </p><p>
     * These are the only permitted mask directions as well as their associated
     * step size:
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
        if ((kingPos & kingMask) != 0)
            return kingPos & kingMask;

        if (kingPos == 0)
            throw new IllegalStateException("Invalid king mask");

        return getMSB(kingMask, kingPos >>> step, step);
    }

    /**
     * Simple method to return if either the white or black king is no longer
     * there, indicating the game has hit an end state.
     *
     * @return true if both kings on the board; false otherwise.
     */
    public boolean kingMissing() {
        return tempBoards[0][5] == 0 || tempBoards[1][5] == 0;
    }

    /**
     * <p>
     * Updates each of king's masks corresponding to the color of the move. This
     * method will be called iff the king from either side is moved, whether by
     * castling or normal means.
     * </p><p>
     * These masks represent the vulnerable squares based on directions the king
     * could be attacked from. When used with isKingInCheck(), the process of
     * seeing whether an attacking piece has a check position on the king
     * becomes trivial.
     * </p><p>
     * The bitwise calculations to determine mask transformations are
     * complicated, but local variables were added to aid in interpretation.
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
        long knights = kingMask[17]; // mask KNIGHT SQUARES

        if (moveDirection == KingMoveDir.UP) {
            kingMask[9]  = newPos ^ kUp;
            kingMask[10] = oldPos | kDown;

            kingMask[11] = kLeft  << 8;
            kingMask[12] = kRight << 8;

            kingMask[13] = diagUL << 8;
            kingMask[14] = diagUR << 8;

            kingMask[15] = (oldPos >>> 1) | (diagLL << 8);
            kingMask[16] = (oldPos << 1)  | (diagLR << 8);

            kingMask[17] = (knights << 8) | (oldPos << 2) | (oldPos >>> 2) | (oldPos >>> 7) | (oldPos >>> 9);
        }
        else if (moveDirection == KingMoveDir.DOWN) {
            kingMask[9]  = oldPos | kUp;
            kingMask[10] = newPos ^ kDown;

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

            kingMask[13] = (diagUL >>> 1) & ~leftBoundXor;
            kingMask[14] = (oldPos | diagUR) << 8;

            kingMask[15] = (diagLL >>> 1) & ~leftBoundXor;
            kingMask[16] = (oldPos | diagLR) >>> 8;

            kingMask[17] = (knights >>> 1) & ~leftBoundXor;
        }
        else if (moveDirection == KingMoveDir.RIGHT) {
            kingMask[9]  = kUp   << 1;
            kingMask[10] = kDown << 1;

            kingMask[11] = kLeft | oldPos;
            kingMask[12] = kRight ^ newPos;

            kingMask[13] = (oldPos | diagUL) << 8;
            kingMask[14] = (diagUR << 1) & ~rightBoundXor;

            kingMask[15] = (oldPos | diagLL) >>> 8;
            kingMask[16] = (diagLR << 1) & ~rightBoundXor;

            kingMask[17] = (knights << 8) & ~rightBoundXor;
        }
        else if (moveDirection == KingMoveDir.UPPER_LEFT) {
            kingMask[9]  = (kUp >>> 1) ^ newPos;
            kingMask[10] = (kDown | oldPos) >>> 1;

            kingMask[11] = (kLeft << 8) ^ newPos;
            kingMask[12] = (kRight | oldPos) << 8;

            kingMask[13] = diagUL ^ newPos;
            kingMask[14] = (oldPos | diagUR) << 16;

            kingMask[15] = ((oldPos | diagLL) >>> 2) & ~(leftBoundXor | (leftBoundXor >>> 1));
            kingMask[16] = diagLR | oldPos;

            kingMask[17] = (((knights << 7) | (oldPos >>> 3) | (oldPos >>> 10)) & ~(leftBoundXor | (leftBoundXor >>> 1)))
                    | (oldPos << 24)  | (oldPos >>> 8)
                    | (((oldPos << 1) | (oldPos << 17)) & ~rightBoundXor) ;
        }
        else if (moveDirection == KingMoveDir.UPPER_RIGHT) {
            kingMask[9]  = (kUp << 1) ^ newPos;
            kingMask[10] = (kDown | oldPos) << 1;

            kingMask[11] = (kLeft | oldPos) << 8;
            kingMask[12] = (kRight << 8) ^ newPos;

            kingMask[13] = (oldPos | diagUL) << 16;
            kingMask[14] = diagUR ^ newPos;

            kingMask[15] = diagLL | oldPos;
            kingMask[16] = ((oldPos | diagLR) << 2) & ~(rightBoundXor | (rightBoundXor << 1));

            kingMask[17] = (((oldPos >>> 1) | (oldPos << 15)) & ~leftBoundXor)
                    | (oldPos >>> 8) | (oldPos << 24)
                    | (((oldPos << 3) | (knights << 9) | (oldPos >>> 6)) & ~(rightBoundXor | (rightBoundXor << 1)));
        }
        else if (moveDirection == KingMoveDir.LOWER_LEFT) {
            kingMask[9]  = (kUp | oldPos) >>> 1;
            kingMask[10] = (kDown >>> 1) ^ newPos;

            kingMask[11] = (kLeft >>> 8) ^ newPos;
            kingMask[12] = (kRight | oldPos) >>> 8;

            kingMask[13] = ((oldPos | diagUL) >>> 2) & ~(leftBoundXor | (leftBoundXor >>> 1));
            kingMask[14] = oldPos | diagUR;

            kingMask[15] = diagLL ^ newPos;
            kingMask[16] = (oldPos | diagLR) >>> 16;

            kingMask[17] = (((knights >>> 9) | (oldPos >>> 3) | (oldPos << 6)) & ~(leftBoundXor | (leftBoundXor >>> 1)))
                    | (oldPos >>> 24) | (oldPos << 8)
                    | (((oldPos << 1) | (oldPos >>> 15)) & ~rightBoundXor);
        }
        else {// moveDirection == KingMoveDir.LOWER_RIGHT
            kingMask[9]  = (kUp | oldPos) << 1;
            kingMask[10] = (kDown << 1) ^ newPos;

            kingMask[11] = (kLeft | oldPos) >>> 8;
            kingMask[12] = (kRight >>> 8) ^ newPos;

            kingMask[13] = ((oldPos | diagUR) << 2) & ~(rightBoundXor | (rightBoundXor << 1));
            kingMask[14] = diagUL | oldPos;

            kingMask[15] = (oldPos | diagLL) >>> 16;
            kingMask[16] = diagLR ^ newPos;

            kingMask[17] = (((oldPos >>> 1) | (oldPos >>> 17))  & ~leftBoundXor)
                    | (oldPos << 8) | (oldPos >>> 24)
                    | (((knights >>> 7) | (oldPos << 3) | (oldPos << 10)) & ~(rightBoundXor | rightBoundXor << 1));
        }

        // Make sure to update these changes to the change history stack
        for (int i = 9; i <= 17; i++)
            appendChange(colorIndex, i);
    }

    /**
     * Given a from and to position, this returns the KingMoveDir enum
     * corresponding to the king's local axis of movement. If the king's
     * movement is not one a legal movement, an exception will be thrown, as
     * this should never occur under normal circumstances.
     *
     * @param from
     *      single bit for king's original position.
     * @param to
     *      single bit for king's new position.
     * @return KingMoveDir
     */
    private KingMoveDir findKingDirection(long from, long to) {
        if (to == (from << 8))
            return KingMoveDir.UP;
        else if (to == (from >>> 8))
            return KingMoveDir.DOWN;
        else if (to == (from >>> 1))
            return KingMoveDir.LEFT;
        else if (to == (from << 1))
            return KingMoveDir.RIGHT;
        else if (to == (from << 7))
            return KingMoveDir.UPPER_LEFT;
        else if (to == (from << 9))
            return KingMoveDir.UPPER_RIGHT;
        else if (to == (from >>> 9))
            return KingMoveDir.LOWER_LEFT;
        else if (to == (from >>> 7))
            return KingMoveDir.LOWER_RIGHT;

        throw new IllegalStateException("King move was illegal!");
    }

    /**
     * When castling the king, instead of using multiple moves to represent the
     * change in masking bits, the default values for both king and queen side
     * masks have been precomputed. Therefore, a simple arraycopy will replace
     * the nine king masks to the default values.
     *
     * @param move
     *      castling the king.
     */
    private void updateKingMasksCastle(Move move) {
        if (move.getCastledRook() == Move.CastleSide.NONE)
            throw new IllegalStateException("Passed a non-castled move to the update castle mask method!");

        int colorIndex = move.getMovedPieceColor().ordinal();
        if (move.getCastledRook() == Move.CastleSide.QUEEN_SIDE)
            System.arraycopy(defaultQueenSideCastleMasks[colorIndex], 0, tempBoards[colorIndex], 9, 9);
        else // move.getCastledRook() == Move.CastleSide.KING_SIDE
            System.arraycopy(defaultKingSideCastleMasks[colorIndex], 0, tempBoards[colorIndex], 9, 9);

        // Update the change history stack
        for (int i = 9; i <= 17; i++)
            appendChange(colorIndex, i);
    }


    /*  === PRINTING METHODS ===*/

    /**
     * Boards printer used for testing purposes.
     */
    public void printGameBoard() {
        char[][] pieces = buildDefaultPieces();
        for (int i = 0; i < 2; i++)
            populate(pieces, i);

        System.out.println(prettyPrintPieces(pieces));
    }

    /**
     * Populates the pieces wit corresponding to the ordinal value of each piece
     * at the given color index.
     *
     * @param pieces
     *      8x8 2D blank char array
     * @param colorIndex
     *      0 -> white<br>1 -> black
     */
    private void populate(char[][] pieces, int colorIndex) {
        populatePieces(pieces, tempBoards[colorIndex][0],'p');
        populatePieces(pieces, tempBoards[colorIndex][1],'N');
        populatePieces(pieces, tempBoards[colorIndex][2],'B');
        populatePieces(pieces, tempBoards[colorIndex][3],'R');
        populatePieces(pieces, tempBoards[colorIndex][4],'Q');
        populatePieces(pieces, tempBoards[colorIndex][5],'K');
    }

    /**
     * Pretty prints the board for a specific color
     *
     * @param color
     *      board to print.
     */
    public void printColorBoard(Piece.Color color) {
        int colorIndex = color.ordinal();
        char[][] pieces = buildDefaultPieces();
        populate(pieces, colorIndex);

        System.out.println(prettyPrintPieces(pieces));
    }

    /**
     * Represents any input mask on the board by pretty printing the output.
     * Does not need to relate to the bitboards themselves.
     *
     * @param mask
     *      to print the positions for.
     */
    public void printMask(long mask) {
        char[][] pieces = buildDefaultPieces();
        populatePieces(pieces, mask, 'X');

        System.out.println(prettyPrintPieces(pieces));
    }

    /**
     * Prints all the king masks for the specific color.
     *
     * @param color
     *      to print the king masks for.
     */
    public void printKingMasks(Piece.Color color) {
        int colorIndex = color.ordinal();
        char[][] pieces = buildDefaultPieces();
        for (int i = 9; i < 17; i++)
            populatePieces(pieces, tempBoards[colorIndex][i], '#');

        populatePieces(pieces, tempBoards[colorIndex][17],'*');
        populatePieces(pieces, tempBoards[colorIndex][5],'K');

        System.out.println(prettyPrintPieces(pieces));
    }

    /**
     * Helper method that will build a string output using a 2D array.
     *
     * @param p
     *      2D array representative of the 8x8 chess board
     * @return String built up chess board
     */
    private String prettyPrintPieces(char[][] p) {
        StringBuilder printStr = new StringBuilder("   _______________________________\n");
        for (int i = 7; i >= 1; i--) {
            printStr.append(String.format("%d | %c | %c | %c | %c | %c | %c | %c | %c |%n", (i + 1), p[i][0], p[i][1], p[i][2],
                    p[i][3], p[i][4], p[i][5], p[i][6], p[i][7]));
            printStr.append("  |---|---|---|---|---|---|---|---|\n");
        }
        printStr.append(String.format("1 | %c | %c | %c | %c | %c | %c | %c | %c |%n", p[0][0], p[0][1], p[0][2], p[0][3], p[0][4],
                p[0][5], p[0][6], p[0][7]));
        printStr.append("   --- --- --- --- --- --- --- --- \n");
        printStr.append("    a   b   c   d   e   f   g   h\n");

        return printStr.toString();
    }

    /**
     * Takes the mask, appends the icon to replace the default white space to
     * indicate that a piece exists at that location. Fairly straight forward,
     * mainly used for debugging and testing purposes.
     *
     * @param pieces
     *      8x8 2D char board for printing.
     * @param mask
     *      long for piece locations on the board
     * @param icon
     *      character to represent the masking bits.
     */
    private void populatePieces(char[][] pieces, long mask, char icon) {
        long rankIter = 0x00FFL; // rank 1
        long fileIter = 0x0101010101010101L;// file A

        for (int r = 0; r < 8; r++) {
            if ((mask & rankIter) != 0) {
                long tempMask = mask & rankIter;
                for (int f = 0; f < 8; f++) {
                    if ((tempMask & fileIter) != 0)
                        pieces[r][f] = icon;
                    fileIter = fileIter << 1;
                }
            }

            rankIter = rankIter << 8; // next rank
            fileIter = 0x0101010101010101L; // reset fileIter
        }

    }

    /**
     * Prints the 1s and 0s that make up the bitboard backing storage. This was
     * implemented as a great way of showing how the underlying process works
     * in leveraging the abilities of 64 bit longs.
     */
    public void printGameBoardBits() {
        char[][] pieces = buildNonDefaultPieces('0');

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 8; j++)
                populatePieces(pieces, tempBoards[i][j],'1');

        prettyPrintPieces(pieces);
        System.out.println("Game Board: " + GameEngine.hexString(gameBoard));
    }

    /**
     * Helper for the printing methods that fills the character array with the
     * default char 'c' and returns the 8x8 2D array.
     *
     * @param c
     *      default value for filling 2D array (typically b' ')
     * @return filled char[][]
     */
    private char[][] buildNonDefaultPieces(char c) {
        char[][] pieces = new char[8][];
        for (int i = 0; i < 8; i++) {
            pieces[i] = new char[]{c, c, c, c, c, c, c, c};
        }

        return pieces;
    }

    /**
     * Builds up the pieces double char array for printing purposes. This action
     * isn't efficient, nor intended as a final implementation for physical use.
     * Serves exclusively as a helpful visual for testing purposes.
     *
     * @return 8x8 2D character array of blank spaces
     */
    private char[][] buildDefaultPieces() {
        return buildNonDefaultPieces(' ');
    }

    /**
     * Returns a string for all relevant boards in a human-readable format that
     * are useful for the purposes of debugging.
     *
     * @param p
     *      2D char array representing both white and black pieces.
     * @param w
     *      2D char array representing white pieces.
     * @param b
     *      2D char array representing black pieces.
     * @return String
     */
    private String prettyPrintAllPieces(char[][] p, char[][] w, char[][] b) {
        StringBuilder printStr = new StringBuilder("\n           GAME BOARD                 #             WHITE BOARD" +
                "                 #             BLACK BOARD               \n");
        printStr.append("   _______________________________    #      _______________________________    #      _____" +
                "__________________________  \n");
        for (int i = 7; i >= 1; i--) {
            printStr.append(String.format("%d | %c | %c | %c | %c | %c | %c | %c | %c |", (i + 1), p[i][0], p[i][1],
                    p[i][2], p[i][3], p[i][4], p[i][5], p[i][6], p[i][7]));
            printStr.append(String.format("   #   %d | %c | %c | %c | %c | %c | %c | %c | %c |", (i + 1), w[i][0],
                    w[i][1], w[i][2], w[i][3], w[i][4], w[i][5], w[i][6], w[i][7]));
            printStr.append(String.format("   #   %d | %c | %c | %c | %c | %c | %c | %c | %c |%n", (i + 1), b[i][0],
                    b[i][1], b[i][2], b[i][3], b[i][4], b[i][5], b[i][6], b[i][7]));
            printStr.append("  |---|---|---|---|---|---|---|---|   #     |---|---|---|---|---|---|---|---|   #     |-" +
                    "--|---|---|---|---|---|---|---|\n");
        }

        printStr.append(String.format("1 | %c | %c | %c | %c | %c | %c | %c | %c |", p[0][0], p[0][1], p[0][2], p[0][3],
                p[0][4], p[0][5], p[0][6], p[0][7]));
        printStr.append(String.format("   #   1 | %c | %c | %c | %c | %c | %c | %c | %c |", w[0][0], w[0][1], w[0][2],
                w[0][3], w[0][4], w[0][5], w[0][6], w[0][7]));
        printStr.append(String.format("   #   1 | %c | %c | %c | %c | %c | %c | %c | %c |%n", b[0][0], b[0][1], b[0][2],
                b[0][3], b[0][4], b[0][5], b[0][6], b[0][7]));
        printStr.append("   --- --- --- --- --- --- --- ---    #      --- --- --- --- --- --- --- ---    #      --- -" +
                "-- --- --- --- --- --- ---  \n");
        printStr.append("    a   b   c   d   e   f   g   h     #       a   b   c   d   e   f   g   h     #       a   " +
                "b   c   d   e   f   g   h   \n");

        return printStr.toString();
    }

    /**
     * Simple toString() implementation that invokes the pretty printing methods
     * to return a combined string with the three different boards (complete,
     * white, and black), as well as the masking bits for the kings of both
     * associated colors.
     *
     * @return human-readable string.
     */
    @Override
    public String toString() {
        char[][] pieces = buildDefaultPieces();
        char[][] whitePieces = buildDefaultPieces();
        char[][] blackPieces = buildDefaultPieces();

        populate(pieces, 0);
        populate(pieces, 1);
        populate(whitePieces, 0);
        populate(blackPieces, 1);

        char[][] whiteMasks = buildDefaultPieces();
        for (int i = 9; i < 17; i++)
            populatePieces(whiteMasks, tempBoards[0][i], '#');
        populatePieces(whiteMasks, tempBoards[0][17],'*');
        populatePieces(whiteMasks, tempBoards[0][5],'K');

        char[][] blackMasks = buildDefaultPieces();
        for (int i = 9; i < 17; i++)
            populatePieces(whiteMasks, tempBoards[0][i], '#');
        populatePieces(blackMasks, tempBoards[1][17],'*');
        populatePieces(blackMasks, tempBoards[1][5],'K');

        String masks = "\n------------ WHITE KING MASKS ----------------\n" + prettyPrintPieces(whiteMasks) +
                       "\n------------ BLACK KING MASKS ----------------\n" + prettyPrintPieces(blackMasks);

        return prettyPrintAllPieces(pieces, whitePieces, blackPieces) + masks;
    }
}