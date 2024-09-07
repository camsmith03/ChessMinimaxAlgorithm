package com.github.camsmith03;

/**
 * Acts as the interpreter for InputLexer that will handle interfacing with the
 * backend bitboard for any parsed input data. This is primarily used to
 * abstract some of the lower end procedures away that are required to verify
 * move validity. Lexer will only handle input grammar, whereas the analyzer
 * exists to ensure boardController legality from the chess notation, throwing exceptions
 * for any discovered violations.
 *
 * @author Cameron Smith
 * @version 08.08.2024
 */
public class InputAnalyzer {
    private final Bitboard board;
    private final long[][] bitboards;
    private final long[] ranks = new long[]{ 0x00000000000000FFL, 0x000000000000FF00L, 0x0000000000FF0000L,
            0x00000000FF000000L, 0x000000FF00000000L, 0x0000FF0000000000L, 0x00FF000000000000L, 0xFF00000000000000L };
    private final long[] files = new long[]{ 0x0101010101010101L, 0x0202020202020202L, 0x0404040404040404L,
            0x0808080808080808L, 0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L };

    /**
     * Constructor for the input analyzer.
     * TODO: color is the same throughout execution, so instead of passing it,
     *       we should use constant fields.
     *
     * @param board
     *      bitboard to be used throughout the lifetime of this class.
     */
    public InputAnalyzer(Bitboard board) {
        this.board = board;
        this.bitboards = board.getBoards();
    }

    /**
     * Method to interpret and verify legality for a king move being made.
     *
     * @param color
     *      of the current moved king.
     * @param destFile
     *      of the king.
     * @param destRank
     *      of the king.
     * @return Move if deemed legal.
     */
    public Move analyzeKingMove(Piece.Color color, int destFile, int destRank) {
        long toMask = getMask(destFile, destRank);
        long kingPos = board.getColorBoards(color)[5];
        long possibleSquare = maskGenerator(toMask, board.getBoardColor(color), kingPos, Piece.Type.KING);

        if ((kingPos & possibleSquare) == 0) {
            throw new IllegalStateException("King position is out of range");
        }
        Move move = new Move(kingPos, toMask, Piece.Type.KING, color);
        ensureLegal(move);

        return move;
    }


    /**
     * <p>
     * Analyze method for either the Knight(N), Bishop(B), Rook(R), or Queen(Q).
     * These were combined into one method since the lexer could leverage that
     * all three have the same input notation, aside from the first letter.
     * </p><p>
     * This version is used for non-ambiguous moves where the origin piece
     * should be easily determined (assuming input is valid).
     * Examples include: Ne4, Bxe4, Re4, Qb5
     * </p>
     *
     * @param color
     *      of the current turn.
     * @param type
     *      either knight, bishop, rook, or queen.
     * @param capturedPiece
     *      flag representing whether a piece exists at the destination square.
     * @param destFile
     *      is a zero-based destination file index.
     * @param destRank
     *      is a zero-based destination rank index.
     * @return Move if  deemed legal.
     *
     * @throws IllegalStateException
     *      Thrown if the Move isn't valid with the current boardController configuration.
     */
    public Move analyzeNBRQ(Piece.Color color, Piece.Type type, boolean capturedPiece, int destFile, int destRank)
                            throws IllegalStateException {

        long toMask = getMask(destFile, destRank);
        long pieceBoard = board.getColorBoards(color)[type.ordinal()];
        long oppBoard; long colorBoard;

        if (color == Piece.Color.WHITE) {
            colorBoard = board.getBoardColor(Piece.Color.WHITE);
            oppBoard = board.getBoardColor(Piece.Color.BLACK);
        }
        else {
            colorBoard = board.getBoardColor(Piece.Color.BLACK);
            oppBoard = board.getBoardColor(Piece.Color.WHITE);
        }

        if ((colorBoard & toMask) != 0) {
            throw new IllegalStateException("Invalid move (piece of same color already in destination square)");
        }

        long fromMask = maskGenerator(toMask, colorBoard, pieceBoard, type);

        if ((fromMask & -fromMask) != fromMask) {
            throw new IllegalStateException("Invalid move (make sure piece ambiguity is specified)");
        }

        Piece movedPiece = board.getPiece(fromMask);

        if (movedPiece == null || movedPiece.type != type) {
            throw new IllegalStateException("Invalid move (piece of the given type doesn't exist at expected source location)");
        }

        if (capturedPiece) {
            if ((oppBoard & toMask) == 0) {
                throw new IllegalStateException("Invalid move (input attempts capture on an empty square)");
            }
            Piece capturedPieceObj = board.getPiece(toMask);

            Move move = new Move(fromMask, toMask, type, color, capturedPieceObj.type);
            ensureLegal(move);

            return move;

        }
        else if ((oppBoard & toMask) != 0) {
            // if destination is occupied by opponent piece yet capturedPiece is false, throw exception
            throw new IllegalStateException("Invalid move (make sure to use an 'x' to designate a piece capture)");
        }

        // no capture, move is legal if check passes
        Move move = new Move(fromMask, toMask, type, color);
        ensureLegal(move);

        return move;
    }



    /**
     * <p>
     *     Analyze method for either the Knight(N), Bishop(B), Rook(R), or Queen(Q) with single ambiguity.
     * </p><p>
     *     This version is used when two pieces of the same type could have moved to the capture location. Thus leading
     *     to ambiguity, requiring a third "source" index. The source index will be file-based if the two pieces have
     *     the same rank, and rank-based if the two pieces have the same file. This also adds some complication that can
     *     lead to input edge cases that need to be considered.
     *     <br>
     *     Examples include: Nbc3, R4xf7
     * </p>
     *
     * @param color
     *      Color of the current turn.
     * @param type
     *      Piece type (either knight, bishop, rook, or queen)
     * @param capturedPiece
     *      Boolean representing whether a piece exists at the destination square.
     * @param destFile
     *      Zero-based destination file index.
     * @param destRank
     *      Zero-based destination rank index.
     * @param source
     *      Zero-based source index (either file or rank based on bool param)
     * @param sourceIsFile
     *      Boolean that is true if the source index is file-based; false if rank-based.
     * @throws IllegalStateException
     *      Thrown if the Move isn't valid with the current boardController configuration.
     * @return Move if it is valid
     */
    public Move analyzeNBRQ(Piece.Color color, Piece.Type type, boolean capturedPiece, int destFile, int destRank, int source,
                           boolean sourceIsFile) throws IllegalStateException {

        long toMask = getMask(destFile, destRank);
        long pieceBoard = board.getColorBoards(color)[type.ordinal()];
        long oppBoard; long colorBoard;

        if (color == Piece.Color.WHITE) {
            colorBoard = board.getBoardColor(Piece.Color.WHITE);
            oppBoard = board.getBoardColor(Piece.Color.BLACK);
        }
        else {
            colorBoard = board.getBoardColor(Piece.Color.BLACK);
            oppBoard = board.getBoardColor(Piece.Color.WHITE);
        }

        if ((colorBoard & toMask) != 0) {
            throw new IllegalStateException("Invalid move (piece of same color already in destination square)");
        }

        long possibleFromLoc = maskGenerator(toMask, colorBoard, pieceBoard, type);
        if (possibleFromLoc == 0) {
            throw new IllegalStateException("Invalid move (piece not able to be moved to the specified destination square)");
        }

        long[] sourceRef;
        if (sourceIsFile) {
            sourceRef = files;
        } else {
            sourceRef = ranks;
        }

        long fromMask = possibleFromLoc & sourceRef[source];

        if ((fromMask & -fromMask) != fromMask) {
            throw new IllegalStateException("Invalid move (piece ambiguity not properly specified, make sure rank or file (or both) of difference is specified)");
        }

        if (capturedPiece) {
            if ((oppBoard & toMask) == 0) {
                throw new IllegalStateException("Invalid move (input attempts capture on an empty square)");
            }

            Piece capturedPieceObj = board.getPiece(toMask);

            Move move = new Move(fromMask, toMask, type, color, capturedPieceObj.type);
            ensureLegal(move);

            return move;
        }
        else if ((oppBoard & toMask) != 0) {
            throw new IllegalStateException("Invalid move (make sure to mark any captured squares with 'x')");
        }

        Move move = new Move(fromMask, toMask, type, color);
        ensureLegal(move);

        return move;
    }

    /**
     * <p>
     *     Analyze method for either the Knight(N), Bishop(B), Rook(R) or Queen(Q) with double ambiguity.
     * </p><p>
     *     This version is used when three (or more) pieces of the same type could be moved to the captured location.
     *     Although unlikely that a non-queen promotion would lead to this condition, it is possible, and thus needs to
     *     remain a consideration. Both a source file and rank are needed that indicate the exact square of the original
     *     piece location.
     * </p>
     *
     * @param color
     *      Color of the current turn.
     * @param type
     *      Piece type (either knight, bishop, rook, or queen)
     * @param capturedPiece
     *      Boolean representing whether a piece exists at the destination square.
     * @param destFile
     *      Zero-based destination file index.
     * @param destRank
     *      Zero-based destination rank index.
     * @param sourceFile
     *      Zero-based source rank index.
     * @param sourceRank
     *      Zero-based source file index.
     * @throws IllegalStateException
     *      Thrown if the Move isn't valid with the current boardController configuration.
     * @return Move if it is valid
     */
    public Move analyzeNBRQ(Piece.Color color, Piece.Type type, boolean capturedPiece, int destFile, int destRank,
                           int sourceFile, int sourceRank) throws IllegalStateException {
        if (sourceFile == destFile && sourceRank == destRank) {
            throw new IllegalStateException("Invalid move (piece cannot be moved to the same square)");
        }
        long toMask = getMask(destFile, destRank);
        long pieceBoard = board.getColorBoards(color)[type.ordinal()];
        long oppBoard; long colorBoard;

        if (color == Piece.Color.WHITE) {
            colorBoard = board.getBoardColor(Piece.Color.WHITE);
            oppBoard = board.getBoardColor(Piece.Color.BLACK);
        }
        else {
            colorBoard = board.getBoardColor(Piece.Color.BLACK);
            oppBoard = board.getBoardColor(Piece.Color.WHITE);
        }


        if ((colorBoard & toMask) != 0) {
            throw new IllegalStateException("Invalid move (piece of same color already in destination square)");
        }
        long masks = maskGenerator(toMask, colorBoard, pieceBoard, type);

        long fromMask = masks & (files[sourceFile] & ranks[sourceRank]); // should be exactly on the given piece

        if (fromMask == 0) {
            throw new IllegalStateException("Invalid move (piece double ambiguity not finding source piece location)");
        }

        if (capturedPiece) {
            if ((oppBoard & toMask) == 0)
                throw new IllegalStateException("Invalid move (input attempts capture on an empty square)");

            Piece capturedPieceObj = board.getPiece(toMask);

            Move move = new Move(fromMask, toMask, type, color, capturedPieceObj.type);
            ensureLegal(move);

            return move;
        }
        else if ((oppBoard & toMask) != 0)
            throw new IllegalStateException("Invalid move (make sure to mark any captured squares with 'x')");


        Move move = new Move(fromMask, toMask, type, color);
        ensureLegal(move);

        return move;
    }


    /**
     * Method to check the validity of a pawn (non-capture) move.
     *
     * @param color
     *      of the moved pawn.
     * @param destFile
     *      of the moved pawn.
     * @param destRank
     *      of the moved pawn.
     * @return Move if deemed legal.
     */
    public Move analyzePawn(Piece.Color color, int destFile, int destRank) {
        long pawnTo = getMask(destFile, destRank);
        long pawnFrom;

        if (color == Piece.Color.WHITE) {
            if ((pawnTo & 0x00000000FF000000L) != 0) {
                // pawn can move two up legally, must check for both conditions
                Piece pawn1 = board.getPiece(pawnTo >>> 8);
                Piece pawn2 = board.getPiece(pawnTo >>> 16);
                if (pawn1 == null) {
                    if (pawn2 == null)
                        throw new IllegalStateException("White pawn move is not legal");

                    pawnFrom = pawnTo >>> 16;
                }
                else
                    pawnFrom = pawnTo >>> 8;
            }
            else {
                Piece pawn = board.getPiece(pawnTo >>> 8);
                pawnFrom = pawnTo >>> 8;
                if (pawn == null)
                    throw new IllegalStateException("White pawn move is not legal");
            }
        }
        else {
            if ((pawnTo & 0x000000FF00000000L) != 0) {
                // pawn can move down two legally, must check for both conditions
                Piece pawn1 = board.getPiece(pawnTo << 8);
                Piece pawn2 = board.getPiece(pawnTo << 16);

                if (pawn1 == null) {
                    if (pawn2 == null)
                        throw new IllegalStateException("Black pawn move is not legal");

                    pawnFrom = pawnTo << 16;
                }
                else
                    pawnFrom = pawnTo << 8;
            }
            else {
                Piece pawn = board.getPiece(pawnTo << 8);
                pawnFrom = pawnTo << 8;
                if (pawn == null)
                    throw new IllegalStateException("Black pawn move is not legal");
            }
        }

        Move move = new Move(pawnFrom, pawnTo, Piece.Type.PAWN, color);
        ensureLegal(move);

        return move;

    }

    /**
     * Checks to see if the arguments used correspond to a move that can be made
     * for the pawns belonging to the Piece.Color.
     *
     * @param color
     *      of the side capturing the piece.
     * @param destFile
     *      index of the destination file (zero-based).
     * @param destRank
     *      index of the destination rank (zero-based).
     * @param sourceFile
     *      index of the moved pawn's origin file (zero-based)
     * @return Move if deemed legal.
     *
     * @throws IllegalStateException
     *      If the pawn capture isn't possible for the current boardController
     *      configuration.
     */
    public Move analyzePawnCapture(Piece.Color color, int destFile, int destRank, int sourceFile) throws IllegalStateException {
        int sourceRank = color == Piece.Color.WHITE ? destRank - 1 : destRank + 1;
        long pawnFrom = getMask(sourceFile, sourceRank);

        if ( (bitboards[color.ordinal()][0] & pawnFrom) == 0)
            throw new IllegalStateException("Invalid pawn start position"); // Invoked color pawn doesn't exist at the
                                                                            // source location
        long captured = getMask(destFile, destRank);
        Piece capturedPiece = board.getPiece(captured);

        if (capturedPiece == null)
            throw new IllegalStateException("Pawn capture square is empty (or e.p. not included in the input)");

        if (capturedPiece.color == color)
            throw new IllegalStateException("Pawn capture square can't be the same color");

        Move move = new Move(pawnFrom, captured, Piece.Type.PAWN, color, capturedPiece.type);
        ensureLegal(move);

        return move;
    }

    /**
     * TODO: modify such that InputLexer will provide the necessary arguments to
     *       avoid the conversions made here.
     */
    public Move analyzePawnEnPassant(String input, Piece.Color color) {
        long captured = getMask(input.charAt(2), getAsciiInt(input.charAt(3)));

        int rank;
        if (color == Piece.Color.WHITE)
            rank = getAsciiInt(input.charAt(3)) - 1;
        else
            rank = getAsciiInt(input.charAt(3)) + 1;


        long pawnFrom = getMask(input.charAt(0), rank);
        Piece pawnLoc = board.getPiece(pawnFrom);

        if (pawnLoc == null)
            throw new IllegalStateException("Invalid pawn start position");

        if (pawnLoc.color != color)
            throw new IllegalStateException("Invalid pawn start location");



        if ((captured & board.getGameBoard()) == 0) {
            // ensure that square is free
            long passantPiece;
            if (color == Piece.Color.WHITE)
                passantPiece = captured >>> 8;
            else
                passantPiece = captured << 8;

            Piece capturedPiece = board.getPiece(passantPiece);

            if (capturedPiece != null) {
                // we now know an enemy piece exists in a potential en passant position we can leverage.

                // Check the enPassantBoard to ensure the piece in question has been marked from the previous turn.
                long enPassantBoard = board.enPassantBoard;
                if ((enPassantBoard & passantPiece) == 0)
                    throw new IllegalStateException("Invalid en passant captured location");

                // At this point, we can confirm the piece has been captured and the move was successful.
                Move enPassantMove = new Move(pawnFrom, captured, Piece.Type.PAWN, color, Piece.Type.PAWN);

                // We have to set the enPassantCaptured bit in the Move object to signify that there was a capture to
                // avoid the piece be assumed to have been captured at the "pawnTo" location.
                enPassantMove.setEnPassant(passantPiece);
                ensureLegal(enPassantMove);

                return enPassantMove;
            }
        }
        throw new IllegalStateException("Invalid en passant move: " + input);
    }


    // TODO: Allow for this move to be made by the player
    public Move analyzePromotedPawn(String input, Piece.Color color, boolean pieceCaptured, char promotedPiece) {
        System.out.println("UNSUPPORTED MOVE");
        System.exit(1);
        return null;
    }

    // TODO: Allow for this move to be made by the player
    public Move analyzeCastle(boolean kingSideCastle, Piece.Color color) {
        System.out.println("UNSUPPORTED MOVE");
        System.exit(1);
        return null;
    }

    /**
     * When invoked with a single square as the destination. It will find the
     * masking bits corresponding to pieces of the given type that could legally
     * reach that square. This is essential for piece ambiguity, where multiple
     * pieces of the same type (and color) could reach a given square.
     * TODO: pieceBoard & colorBoard args are not needed.
     *
     * @param dest
     *      bit representing the square the piece is moved to.
     * @param colorBoard
     *      for the current turn.
     * @param pieceBoard
     *      for the current turn.
     * @param type
     *      labeled to move (matching the pieceBoard).
     * @return mask(s) that hold the bits matching the locations for the pieces
     *         of the type and color that could reach the dest.
     */
    private long maskGenerator(long dest, long colorBoard, long pieceBoard, Piece.Type type) {
        long fileA = 0x0101010101010101L;
        long fileB = 0x0202020202020202L;
        long fileG = 0x4040404040404040L;
        long fileH = 0x8080808080808080L;

        if (type == Piece.Type.KNIGHT) {
            return pieceBoard & ((((dest << 10) | (dest >>>  6)) & ~fileB & ~fileA)
                                             | (((dest << 17) | (dest >>> 15)) & ~fileA)
                                             | (((dest << 15) | (dest >>> 17)) & ~fileH)
                                             | (((dest <<  6) | (dest >>> 10)) & ~fileG & ~fileH));
        } else if (type == Piece.Type.BISHOP) {
            return bishopMask(dest) & pieceBoard;

        } else if (type == Piece.Type.ROOK) {
            return rookMask(dest, colorBoard) & pieceBoard;

        } else if (type == Piece.Type.QUEEN) {
            return (bishopMask(dest) | rookMask(dest, colorBoard)) & pieceBoard;

        } else if (type == Piece.Type.KING) {
            long kingMask = (dest >>> 8) | (dest << 8);
            long kingLeft = dest >>> 1;
            if (((kingLeft) & fileH) == 0) {
                kingMask |= (kingLeft | kingLeft << 8 | kingLeft >>> 8);
            }
            long kingRight = dest << 1;
            if (((kingRight) & fileA) == 0) {
                kingMask |= (kingRight | kingRight << 8 | kingRight >>> 8);
            }

            return kingMask & pieceBoard;
        }

        throw new IllegalArgumentException("Piece type is NONE");
    }

    /**
     * Helper method for maskGenerator used for bishops. This will use some
     * fundamental bitwise operations that will precisely find, and return the
     * first pieces along each diagonal. It is analogous to color, and will
     * return multiple masking bits if multiple can be considered. If zero is
     * returned, it can be inferred that no pieces could reach the square.
     * <br>
     * Note: this won't necessarily return any bishops, as a bitwise AND will be
     * used on a piece boardController after invoked. It only returns the pieces that are
     * along the diagonals, that could potentially be bishops
     *
     * @param dest
     *      bit representing the square any bishop was moved to.
     * @return long
     *      value corresponding to the bishop(s) that can reach the dest bit.
     */
    private long bishopMask(long dest) {
        long gameBoard = board.getGameBoard();
        long fileA = 0x0101010101010101L;
        long fileH = 0x8080808080808080L;
        long bishopMask = 0;


        long bishopLeft = dest >>> 1;
        long upperLeft = bishopLeft << 8;
        long lowerLeft = bishopLeft >>> 8;
        // find any bishops on the upper left or lower left diagonals
        while((bishopLeft & fileH) == 0 && bishopLeft != 0) {
            if ((upperLeft & gameBoard) == 0) {
                // stop iteration once the first occurrence is recorded
                upperLeft = upperLeft << 7;
            }
            if ((lowerLeft & gameBoard) == 0) {
                lowerLeft = lowerLeft >>> 9;
            }
            bishopLeft >>>= 1;
        }
        if ((upperLeft & gameBoard) != 0 && (upperLeft & fileH) == 0) {
            bishopMask |= upperLeft;
        }
        if ((lowerLeft & gameBoard) != 0 && (lowerLeft & fileH) == 0) {
            bishopMask |= lowerLeft;
        }

        long bishopRight = dest << 1;
        long upperRight = bishopRight << 8;
        long lowerRight = bishopRight >>> 8;
        // find any bishops on the upper right or lower right diagonals
        while ((bishopRight & fileA) == 0 && bishopRight != 0) {
            if ((upperRight & gameBoard) == 0) {
                // stop iteration once first occurrence is recorded
                upperRight = upperRight << 9;
            }
            if ((lowerRight & gameBoard) == 0) {
                lowerRight = lowerRight >>> 7;
            }
            bishopRight <<= 1;
        }
        if ((upperRight & gameBoard) != 0 && (upperRight & fileA) == 0) {
            bishopMask |= upperRight;
        }
        if ((lowerRight & gameBoard) != 0 && (lowerRight & fileA) == 0) {
            bishopMask |= lowerRight;
        }

        return bishopMask;
    }

    /**
     * Helper method for maskGenerator used exclusively for Rooks. As with the
     * bishopMask method, this will also use fundamental bit manipulation to
     * obtain the rook (or rooks) that could be used to reach the dest square.
     * <br>
     * Note: this won't necessarily return any rooks, as a bitwise AND will be
     * used on a piece boardController after invoked. It only returns the pieces that are
     * along the rank and file of the dest, that could potentially be rooks.
     * TODO: get rid of reliance on colorBoard as its an unnecessary parameter.
     *
     * @param dest
     *      bit representing the square that a rook is being moved to.
     * @param colorBoard
     *      boardController from the color the move is designated to.
     * @return long
     *      mask of pieces along the given file and rank that may be a rook.
     */
    private long rookMask(long dest, long colorBoard) {
        long fileA = 0x0101010101010101L;
        long fileH = 0x8080808080808080L;

        long rookMask = 0;
        long rookLeft = dest >>> 1;
        while((rookLeft & fileH) == 0 && rookLeft != 0) {
            if ((rookLeft & colorBoard) != 0) {
                // obtain the first rook along the given rank
                rookMask |= rookLeft;
                rookLeft = fileH;
            } else {
                rookLeft >>>= 1;
            }
        }
        long rookRight = dest << 1;
        while ((rookRight & fileA) == 0 && rookRight != 0) {
            if ((rookRight & colorBoard) != 0) {
                // obtain the first rook along the given rank to the left
                rookMask |= rookRight;
                rookRight = fileA;
            } else {
                rookRight <<= 1;
            }
        }
        long rookUp = dest << 8;

        while (rookUp != 0) {
            if ((rookUp & colorBoard) != 0) {
                rookMask |= rookUp;
                rookUp = 0;
            } else {
                rookUp <<= 8;
            }
        }

        long rookDown = dest >>> 8;
        while (rookDown != 0) {
            if ((rookDown & colorBoard) != 0) {
                rookMask |= rookDown;
                rookDown = 0;
            } else {
                rookDown >>>= 8;
            }
        }
        return rookMask;
    }

    private long getMask(int file, int rank) {
        return (0x01L << file) << (8 * rank);
    }

    /**
     * Used exclusively by en passant analysis for now.
     * TODO: this is not necessary and other methods can be invoked to achieve
     *       a similar outcome. e.p. currently relies on older style of analysis
     */
    @Deprecated
    private long getMask(char rank, int file) throws IllegalArgumentException {
        long mask = 0x01L;

        int rankOff = ((int) rank) - 97; // ASCII conversion
        if (rankOff > 7 || rankOff < 0) {
            throw new IllegalArgumentException("Invalid rank");
        }

        if (file > 8 || file < 1) {
            throw new IllegalArgumentException("Invalid file");
        }

        return (0x01L << rankOff) << (8 * (file - 1));
    }

    private int getAsciiInt(char c) {
        return ((int) c) - 48;
    }

    /**
     * Utilizes boardController virtualization to apply the given move to the current
     * boardController and see if the king is put in check.
     *
     * @param move
     *      Move to apply to the boardController
     * @throws IllegalStateException
     *      Thrown if it puts the king in check
     */
    private void ensureLegal(Move move) throws IllegalStateException {
        if (!board.isMoveLegal(move)) {
            throw new IllegalStateException("Invalid move (the move attempts to place it's own king in a checked position).");
        }
    }
}