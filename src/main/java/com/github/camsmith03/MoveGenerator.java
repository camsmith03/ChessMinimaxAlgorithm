package com.github.camsmith03;

public class MoveGenerator {
    private final long alternatingByteMask = 0xFF00FF00FF00FF00L;
    private MoveList possibleMoves;
    private Bitboard bitboard;
    private long[][] boards;
//    private long[] colorBoards;
    private final long[] queenSideCastleCheck = new long[]{0x0E, 0xE000000000000000L};
    private final long[] kingSideCastleCheck = new long[]{0x60, 0x06000000000000L};

    /**
     * Generates all moves, even those that may be illegal (putting king in check, etc.)
     *
     */
    public MoveList generateMoves(Bitboard bitboard, Piece.Color turnToMove) {
        possibleMoves = new MoveList();
        this.bitboard = bitboard;
        boards = bitboard.getBoards();
//        colorBoards = new long[]{
//                bitboard.getBoardColor(Piece.Color.WHITE),
//                bitboard.getBoardColor(Piece.Color.BLACK)
//        };

        if (turnToMove == Piece.Color.WHITE) {
            long[] white = boards[0];
            // ==============  Pawns  ==============
            whitePawnAppend(white[0]);
            // ============== Knights ==============
            knightAppend(white[1], Piece.Color.WHITE);
            // ============== Bishops ==============
            bishopAppend(white[2], Piece.Type.BISHOP, Piece.Color.WHITE);
            // ==============  Rooks  ==============
            rookAppend(white[3], Piece.Type.ROOK, Piece.Color.WHITE);
            // ==============  Queen  ==============
            bishopAppend(white[4], Piece.Type.QUEEN, Piece.Color.WHITE);
            rookAppend(white[4], Piece.Type.QUEEN, Piece.Color.WHITE);
            // ==============   King  ==============
            kingAppend(white[5], Piece.Color.WHITE);
        }
        else {
            long[] black = boards[1];
            // ==============  Pawns  ==============
            blackPawnAppend(black[0]);
            // ============== Knights ==============
            knightAppend(black[1], Piece.Color.BLACK);
            // ============== Bishops ==============
            bishopAppend(black[2], Piece.Type.BISHOP, Piece.Color.BLACK);
            // ==============  Rooks  ==============
            rookAppend(black[3], Piece.Type.ROOK, Piece.Color.BLACK);
            // ==============  Queen  ==============
            bishopAppend(black[4], Piece.Type.QUEEN, Piece.Color.BLACK);
            rookAppend(black[4], Piece.Type.QUEEN, Piece.Color.BLACK);
            // ==============   King  ==============
            kingAppend(black[5], Piece.Color.BLACK);
        }

        return possibleMoves;
    }

    private void whitePawnAppend(long pawns) {
        long gameBoard = bitboard.getGameBoard();

        // Move up 1
        long pawnMoves1 = (pawns << 8)  & ~gameBoard;
        while (pawnMoves1 != 0) {
            long pawn = pawnMoves1 & -pawnMoves1;
            if ((pawn & 0xFF00000000000000L) != 0) {
                // Pawn promoted
                possibleMoves.add(pawn >>> 8, pawn, Piece.Type.PAWN, Piece.Color.WHITE, Piece.Type.NONE, Piece.Type.KNIGHT);
                possibleMoves.add(pawn >>> 8, pawn, Piece.Type.PAWN, Piece.Color.WHITE, Piece.Type.NONE, Piece.Type.BISHOP);
                possibleMoves.add(pawn >>> 8, pawn, Piece.Type.PAWN, Piece.Color.WHITE, Piece.Type.NONE, Piece.Type.QUEEN);
            }
            else {
                pieceAppend(pawn >>> 8, pawn, Piece.Type.PAWN, Piece.Color.WHITE);
            }
            pawnMoves1 ^= pawn;
        }

        // Move up 2 (only from starting square)
        long pawnMoves2 = ((pawns & 0xFF00L) << 16) & ~gameBoard;

        while (pawnMoves2 != 0) {
            long pawn = pawnMoves2 & -pawnMoves2;
            pieceAppend(pawn >>> 16, pawn, Piece.Type.PAWN, Piece.Color.WHITE);
            pawnMoves2 ^= pawn;
        }

        // Capture (not en passant)
        long pawnCaptured = pawns;
        long pMask;
        while (pawnCaptured != 0) {
            long pawn = pawnCaptured & -pawnCaptured;
            long pShift = (pawn << 8);
            if ( (pShift & alternatingByteMask) != 0) {
                pMask = alternatingByteMask;
            }
            else {
                pMask = ~alternatingByteMask;
            }

            if (((pShift << 1) & pMask) != 0) {
                // If pawn is not on right edge of the board
                pawnCapture(pawn, pShift << 1, Piece.Color.WHITE);
            }

            if (((pShift >>> 1) & pMask) != 0) {
                // If pawn is not on left edge of the board
                pawnCapture(pawn, pShift >>> 1, Piece.Color.WHITE);
            }
            pawnCaptured ^= pawn;
        }

        // Capture (en passant)
        if (bitboard.enPassantBoard != 0) {
            long whitePawns = pawns & bitboard.enPassantBoard;
            long blackPawn = bitboard.enPassantBoard ^ whitePawns;
            long whitePawn = whitePawns & -whitePawns;

            Move enPassant = new Move(whitePawn, blackPawn << 8, Piece.Type.PAWN, Piece.Color.WHITE, Piece.Type.PAWN);

            enPassant.setEnPassant(blackPawn);
            possibleMoves.addMove(enPassant);


            if (whitePawns != whitePawn) {
                // Double en passant (two white pawns can perform the attack)
                // Rare, but plausible, so condition is here to ensure correct moves generated
                whitePawn ^= whitePawns;
                enPassant = new Move(whitePawn, blackPawn << 8, Piece.Type.PAWN, Piece.Color.WHITE, Piece.Type.PAWN);

                enPassant.setEnPassant(blackPawn);
                possibleMoves.addMove(enPassant);
            }
        }
        /*
        We can apply check for illegal pawn moves by using two alternating byte masks.
            alternatingByteMask  => 0xFF00FF00FF00FF00
            ~alternatingByteMask => 0x00FF00FF00FF00FF

        For pawn movements, any space that is capture-able must have a bit falling within the same mask as the original
        space (after moving up). The masks are inverses of alternating bytes. This behavior isn't obvious at first, but
        one can notice that the 8 squares per file, corresponds to 8 bits. Thus, each file follows the alternating set
        of bytes. So whenever the pawn is on the far left or right rank, we can automatically determine whether a legal
        move is valid based on is file's mask (as looping over would increase or decrease a file thus changing the
        mask).
         */
    }

    private void blackPawnAppend(long pawns) {
        long gameBoard = bitboard.getGameBoard();

        // Move down 1
        long pawnMoves1 = (pawns >>> 8)  & ~gameBoard;
        while (pawnMoves1 != 0) {
            long pawn = pawnMoves1 & -pawnMoves1;

            if ((pawn & 0x00FF) != 0) {
                // Promoted Pawns
                possibleMoves.add(pawn << 8, pawn, Piece.Type.PAWN, Piece.Color.BLACK, Piece.Type.NONE, Piece.Type.KNIGHT);
                possibleMoves.add(pawn << 8, pawn, Piece.Type.PAWN, Piece.Color.BLACK, Piece.Type.NONE, Piece.Type.BISHOP);
                possibleMoves.add(pawn << 8, pawn, Piece.Type.PAWN, Piece.Color.BLACK, Piece.Type.NONE, Piece.Type.QUEEN);
            }
            else {
                pieceAppend(pawn << 8, pawn, Piece.Type.PAWN, Piece.Color.BLACK);
            }
            pawnMoves1 ^= pawn;
        }

        // Move down 2 (only from starting square)
        long pawnMoves2 = ((pawns & 0x00FF000000000000L) >>> 16) & ~gameBoard;
        while (pawnMoves2 != 0) {
            long pawn = pawnMoves2 & -pawnMoves2;
            pieceAppend(pawn << 16, pawn, Piece.Type.PAWN, Piece.Color.BLACK);
            pawnMoves2 ^= pawn;
        }

        // Capture (not en passant)
        long pawnCaptured = pawns;
        long pMask;
        while (pawnCaptured != 0) {
            long pawn = pawnCaptured & -pawnCaptured;
            long pShift = (pawn >>> 8);
            if ( (pShift & alternatingByteMask) != 0) {
                pMask = alternatingByteMask;
            }
            else {
                pMask = ~alternatingByteMask;
            }

            if (((pShift << 1) & pMask) != 0) {
                // If pawn is not on right edge of the board
                pawnCapture(pawn, pShift << 1, Piece.Color.BLACK);
            }

            if (((pShift >>> 1) & pMask) != 0) {
                // If pawn is not on left edge of the board
                pawnCapture(pawn, pShift >>> 1, Piece.Color.BLACK);
            }
            pawnCaptured ^= pawn;
        }

        // Capture (en passant)
        if (bitboard.enPassantBoard != 0) {
            long blackPawns = pawns & bitboard.enPassantBoard;
            long whitePawn = bitboard.enPassantBoard ^ blackPawns;
            long blackPawn = blackPawns & -blackPawns;

            Move enPassant = new Move(blackPawn, whitePawn >>> 8, Piece.Type.PAWN, Piece.Color.BLACK,
                    Piece.Type.PAWN);

            enPassant.setEnPassant(whitePawn);
            possibleMoves.addMove(enPassant);


            if (blackPawns != blackPawn) {
                // Double en passant (two black pawns can perform attack)
                // Rare, but plausible, so condition is here to ensure correct moves generated
                blackPawn ^= blackPawns;
                enPassant = new Move(blackPawn, whitePawn >>> 8, Piece.Type.PAWN, Piece.Color.BLACK,
                        Piece.Type.PAWN);

                enPassant.setEnPassant(whitePawn);
                possibleMoves.addMove(enPassant);
            }
        }
    }


    private void kingAppend(long king, Piece.Color color) {
        long kingMask;
        if ((king & alternatingByteMask) != 0) {
            kingMask = alternatingByteMask;
        }
        else {
            kingMask = ~alternatingByteMask;
        }

        if (((king << 1) & kingMask) != 0) {
            pieceAppend(king, king << 1, Piece.Type.KING, color);
            pieceAppend(king, (king << 1) << 8, Piece.Type.KING, color);
            pieceAppend(king, (king << 1) >>> 8, Piece.Type.KING, color);
        }

        if (((king >>> 1) & kingMask) != 0) {
            pieceAppend(king, king >>> 1, Piece.Type.KING, color);
            pieceAppend(king, (king >>> 1) << 8, Piece.Type.KING, color);
            pieceAppend(king, (king >>> 1) >>> 8, Piece.Type.KING, color);
        }
        pieceAppend(king, king << 8, Piece.Type.KING, color);
        pieceAppend(king, king >>> 8, Piece.Type.KING, color);

        int colorIndex = color.ordinal();
        if (boards[colorIndex][6] != 0) {
            // Check Queen side castle
            long boardColor = bitboard.getBoardColor(color);
            if ((boardColor & queenSideCastleCheck[colorIndex]) == 0) {
                Move queenSideCastle = new Move(0, 0, Piece.Type.KING, color);
                queenSideCastle.setCastledRook(Move.CastleSide.QUEEN_SIDE);
                possibleMoves.addMove(queenSideCastle);
            }

            // Check King side castle
            if ((boardColor & kingSideCastleCheck[colorIndex]) == 0) {
                Move kingSideCastle = new Move(0, 0, Piece.Type.KING, color);
                kingSideCastle.setCastledRook(Move.CastleSide.KING_SIDE);
                possibleMoves.addMove(kingSideCastle);
            }

        }
    }

    private void rookAppend(long rooks, Piece.Type type, Piece.Color color) {
        long rookMask;
        long rookLeft, rookRight, rookUp, rookDown;
        while (rooks != 0) {
            long rook = rooks & -rooks;

            if ((rook & alternatingByteMask) != 0) {
                rookMask = alternatingByteMask;
            }
            else {
                rookMask = ~alternatingByteMask;
            }

            rookLeft = rook >>> 1;
            while ((rookLeft & rookMask) != 0 && rookLeft != 0) {
                if (pieceAppend(rook, rookLeft, type, color) == 0) {
                    rookLeft = rookLeft >>> 1;
                }
                else {
                    rookLeft = 0;
                }
            }

            rookRight = rook << 1;
            while ((rookRight & rookMask) != 0 && rookRight != 0) {
                if (pieceAppend(rook, rookRight, type, color) == 0) {
                    rookRight = rookRight << 1;
                }
                else {
                    rookRight = 0;
                }
            }

            rookUp = rook << 8;
            while (rookUp != 0) {
                if (pieceAppend(rook, rookUp, type, color) == 0) {
                    rookUp = rookUp << 8;
                }
                else {
                    rookUp = 0;
                }
            }

            rookDown = rook >>> 8;
            while (rookDown != 0) {
                if (pieceAppend(rook, rookDown, type, color) == 0) {
                    rookDown = rookDown << 8;
                }
                else {
                    rookDown = 0;
                }
            }
            rooks ^= rook;
        }
    }

    private void bishopAppend(long bishops, Piece.Type type, Piece.Color color) {
        long bishopMask;
        long upperdiag;
        long lowerdiag;

        while (bishops != 0) {
            long bishop = bishops & -bishops;

            if ((bishop & alternatingByteMask) != 0) {
                bishopMask = alternatingByteMask;
            }
            else {
                bishopMask = ~alternatingByteMask;
            }

            upperdiag = bishop << 7;
            lowerdiag = bishop >>> 9;
            long leftSide = bishop >>> 1;
            while ((leftSide & bishopMask) != 0 && (upperdiag != 0 || lowerdiag != 0)) {
                if (upperdiag != 0) {
                    if (pieceAppend(bishop, upperdiag, type, color) == 0) {
                        upperdiag = upperdiag << 7;
                    }
                    else {
                        upperdiag = 0;
                    }
                }
                if (lowerdiag != 0) {
                    if (pieceAppend(bishop, lowerdiag, type, color) == 0) {
                        lowerdiag = lowerdiag >>> 9;
                    }
                    else {
                        lowerdiag = 0;
                    }
                }
                leftSide = leftSide >>> 1;
            }

            upperdiag = bishop << 9;
            lowerdiag = bishop >>> 7;
            long rightSide = bishop << 1;
            while ((rightSide & bishopMask) != 0 && (upperdiag != 0 || lowerdiag != 0)) {
                if (upperdiag != 0) {
                    if (pieceAppend(bishop, upperdiag, type, color) == 0) {
                        upperdiag = upperdiag << 9; // Bishop hit an empty square, can continue on the diagonal.
                    }
                    else {
                        upperdiag = 0; // Bishop hit nonempty square, can no longer continue on the diagonal.
                    }
                }
                if (lowerdiag != 0) {
                    if (pieceAppend(bishop, lowerdiag, type, color) == 0) {
                        lowerdiag = lowerdiag >>> 7; // Bishop hit an empty square, can continue on the diagonal.
                    }
                    else {
                        lowerdiag = 0; // Bishop hit nonempty square, can no longer continue on the diagonal.
                    }
                }
                rightSide = rightSide << 1;
            }

            bishops ^= bishop;
        }
    }

    private void knightAppend(long knights, Piece.Color color) {
        long knightMask;
        while (knights != 0) {
            long knight = knights & -knights;
            if ((knight & alternatingByteMask) != 0) {
                knightMask = alternatingByteMask;
            }
            else {
                knightMask = ~alternatingByteMask;
            }
            if (((knight >>> 1) & knightMask) != 0) {
                if (((knight >>> 2) & knightMask) != 0) {
                    pieceAppend(knight, (knight << 8) >>> 2, Piece.Type.KNIGHT, color);
                    pieceAppend(knight, (knight >>> 8) >>> 2, Piece.Type.KNIGHT, color);
                }
                pieceAppend(knight, (knight << 16) >>> 1, Piece.Type.KNIGHT, color);
                pieceAppend(knight, (knight >>> 16) >>> 1, Piece.Type.KNIGHT, color);
            }

            if (((knight << 1) & knightMask) != 0) {
                if (((knight << 2) & knightMask) != 0) {
                    pieceAppend(knight, (knight << 8) << 2, Piece.Type.KNIGHT, color);
                    pieceAppend(knight, (knight >>> 8) << 2, Piece.Type.KNIGHT, color);
                }
                pieceAppend(knight, (knight << 16) << 1, Piece.Type.KNIGHT, color);
                pieceAppend(knight, (knight >>> 16) << 1, Piece.Type.KNIGHT, color);
            }
            knights ^= knight;
        }
    }

    private void pawnCapture(long oldLoc, long newLoc, Piece.Color color) {
        if (newLoc != 0) {
            Piece captured = bitboard.getPiece(newLoc);
            if (captured != null) {
                if (color != captured.color) {
                    if ((newLoc & 0xFF000000000000FFL) != 0) {
                        // Captured piece AND pawn promotion
                        possibleMoves.addMove(new Move(oldLoc, newLoc, Piece.Type.PAWN, color, captured.type, Piece.Type.KNIGHT));
                        possibleMoves.addMove(new Move(oldLoc, newLoc, Piece.Type.PAWN, color, captured.type, Piece.Type.BISHOP));
                        possibleMoves.addMove(new Move(oldLoc, newLoc, Piece.Type.PAWN, color, captured.type, Piece.Type.QUEEN));
                    }
                    else {
                        // Captured piece
                        possibleMoves.add(oldLoc, newLoc, Piece.Type.PAWN, color, captured.type, Piece.Type.NONE); // opponent piece captured
                    }
                }
            }
        }
    }


    /**
     *
     *
     * @param oldLoc
     *      single bit representing original location where piece started.
     * @param newLoc
     *      single bit representing location that piece was moved to.
     * @param color
     *      color of the piece that is moved
     * @return 0 if new location was an empty space
     *         1 if opponent piece captured at new location
     *        -1 if new location stored invoking player colored piece
     *        -2 if new location is off the board
     */
    private int pieceAppend(long oldLoc, long newLoc, Piece.Type type, Piece.Color color) {
        // check to see if piece move is legal (no piece of same color) and append it whether it has captured
        // a black piece or not. Assumes the locations are valid and checked before function call.
        if (newLoc != 0) {
            // avoid the method call if the newLoc extends below or above the board.
            Piece captured = bitboard.getPiece(newLoc);
            if (captured != null) {
                if (color != captured.color) {
                    possibleMoves.add(oldLoc, newLoc, type, color, captured.type, Piece.Type.NONE); // opponent piece captured
                    return 1;
                }
                return -1;
            }
            else {
                possibleMoves.add(oldLoc, newLoc, type, color, Piece.Type.NONE, Piece.Type.NONE); // empty square
                return 0;
            }
        }
        return -2;
    }
}
