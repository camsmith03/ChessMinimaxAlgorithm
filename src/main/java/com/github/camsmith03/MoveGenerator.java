package com.github.camsmith03;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class MoveGenerator {
    private final long alternatingByteMask = 0xFF00FF00FF00FF00L;
    private ArrayList<Move> possibleMoves;
    private Bitboard bitboard;

    /**
     * Generates all moves, even those that may be illegal (putting king in check, etc.)
     *
     */
    public ArrayList<Move> generateMoves(Board board) {
        possibleMoves = new ArrayList<>();
        bitboard = board.getBitboard();

        if (board.getTurn() == GamePiece.PieceColor.WHITE) {
            long[] white = bitboard.getWhitePieceBitboards();
            // ==============  Pawns  ==============
            whitePawnAppend(white[0]);
            // ============== Knights ==============
            knightAppend(white[1], GamePiece.PieceColor.WHITE);
            // ============== Bishops ==============
            bishopAppend(white[2], GamePiece.PieceColor.WHITE);
            // ==============  Rooks  ==============
            rookAppend(white[3], GamePiece.PieceColor.WHITE);
            // ==============  Queen  ==============
            bishopAppend(white[4], GamePiece.PieceColor.WHITE);
            rookAppend(white[4], GamePiece.PieceColor.WHITE);
            // ==============   King  ==============
            kingAppend(white[5], GamePiece.PieceColor.WHITE);
        }
        else {
            long[] black = bitboard.getBlackPieceBitboards();
            // ==============  Pawns  ==============
            blackPawnAppend(black[0]);
            // ============== Knights ==============
            knightAppend(black[1], GamePiece.PieceColor.BLACK);
            // ============== Bishops ==============
            bishopAppend(black[2], GamePiece.PieceColor.BLACK);
            // ==============  Rooks  ==============
            rookAppend(black[3], GamePiece.PieceColor.BLACK);
            // ==============  Queen  ==============
            bishopAppend(black[4], GamePiece.PieceColor.BLACK);
            rookAppend(black[4], GamePiece.PieceColor.BLACK);
            // ==============   King  ==============
            kingAppend(black[5], GamePiece.PieceColor.BLACK);
        }

        return possibleMoves;
    }

    private void whitePawnAppend(long pawns) {
        long gameBoard = bitboard.getGameBoard();

        // Move up 1
        long pawnMoves1 = (pawns << 8)  & ~gameBoard;
        for (long pawn = pawnMoves1 & -pawnMoves1; pawnMoves1 != 0; pawnMoves1 ^= pawn) {
            pieceAppend(pawn, pawn << 8, GamePiece.PieceColor.WHITE);
        }

        // Move up 2 (only from starting square)
        long pawnMoves2 = ((pawns & 0xFF00L) << 16) & ~gameBoard;
        for (long pawn = pawnMoves2 & -pawnMoves2; pawnMoves2 != 0; pawnMoves2 ^= pawn) {
            pieceAppend(pawn, pawn << 16, GamePiece.PieceColor.WHITE);
        }

        // Capture (not en passant)
        long pawnCapture = pawns;
        long pMask;
        for(long pawn = pawnCapture & -pawnCapture; pawnCapture != 0; pawnCapture ^= pawn) {
            long pShift = (pawn << 8);
            if ( (pShift & alternatingByteMask) != 0) {
                pMask = alternatingByteMask;
            }
            else {
                pMask = ~alternatingByteMask;
            }

            if (((pShift << 1) & pMask) != 0) {
                // If pawn is not on right edge of the board
                pieceAppend(pawn, pShift << 1, GamePiece.PieceColor.WHITE);
            }

            if (((pShift >>> 1) & pMask) != 0) {
                // If pawn is not on left edge of the board
                pieceAppend(pawn, pShift >>> 1, GamePiece.PieceColor.WHITE);
            }
        }
        /*
        We can apply check for illegal pawn moves by using two alternating byte masks.
            alternatingByteMask  => 0xFF00FF00FF00FF00
            ~alternatingByteMask => 0x00FF00FF00FF00FF

        For pawn movements, any space that is capture-able must have a bit falling within the same mask as the original
        space (after moving up). The masks are inverses of alternating bytes. This behavior isn't obvious at first, but
        one can notice that for the 8 squares per file, it corresponds to 8 bits. Thus, each file follows the
        alternating set of bytes. So whenever the pawn is on the far left or right rank, we can automatically determine
        whether a legal move is valid based on is file's mask (as looping over would increase or decrease a file thus
        changing the mask).
         */
    }

    private void blackPawnAppend(long pawns) {
        long gameBoard = bitboard.getGameBoard();

        // Move down 1
        long pawnMoves1 = (pawns >>> 8)  & ~gameBoard;
        for (long pawn = pawnMoves1 & -pawnMoves1; pawnMoves1 != 0; pawnMoves1 ^= pawn) {
            pieceAppend(pawn, pawn >>> 8, GamePiece.PieceColor.BLACK);
        }

        // Move down 2 (only from starting square)
        long pawnMoves2 = ((pawns & 0x00FF000000000000L) >>> 16) & ~gameBoard;
        for (long pawn = pawnMoves2 & -pawnMoves2; pawnMoves2 != 0; pawnMoves2 ^= pawn) {
            pieceAppend(pawn, pawn >>> 16, GamePiece.PieceColor.BLACK);
        }

        // Capture (not en passant)
        long pawnCapture = pawns;
        long pMask;
        for(long pawn = pawnCapture & -pawnCapture; pawnCapture != 0; pawnCapture ^= pawn) {
            long pShift = (pawn >>> 8);
            if ( (pShift & alternatingByteMask) != 0) {
                pMask = alternatingByteMask;
            }
            else {
                pMask = ~alternatingByteMask;
            }

            if (((pShift << 1) & pMask) != 0) {
                // If pawn is not on right edge of the board
                pieceAppend(pawn, pShift << 1, GamePiece.PieceColor.BLACK);
            }

            if (((pShift >>> 1) & pMask) != 0) {
                // If pawn is not on left edge of the board
                pieceAppend(pawn, pShift >>> 1, GamePiece.PieceColor.BLACK);
            }
        }
    }


    private void kingAppend(long king, GamePiece.PieceColor color) {
        long kingMask;
        if ((king & alternatingByteMask) != 0) {
            kingMask = alternatingByteMask;
        }
        else {
            kingMask = ~alternatingByteMask;
        }

        if (((king << 1) & kingMask) != 0) {
            pieceAppend(king, king << 1, color);
            pieceAppend(king, (king << 1) << 8, color);
            pieceAppend(king, (king << 1) >>> 8, color);
        }
        else if (((king >>> 1) & kingMask) != 0) {
            pieceAppend(king, king >>> 1, color);
            pieceAppend(king, (king >>> 1) << 8, color);
            pieceAppend(king, (king >>> 1) >>> 8, color);
        }
        pieceAppend(king, king << 8, color);
        pieceAppend(king, king >>> 8, color);
    }

    private void rookAppend(long rooks, GamePiece.PieceColor color) {
        long rookMask;
        long rookLeft, rookRight, rookUp, rookDown;
        for (long rook = rooks & -rooks; rooks != 0; rooks ^= rook) {
            if ((rook & alternatingByteMask) != 0) {
                rookMask = alternatingByteMask;
            }
            else {
                rookMask = ~alternatingByteMask;
            }

            rookLeft = rook >>> 1;
            while ((rookLeft & rookMask) != 0 && rookLeft != 0) {
                if (pieceAppend(rook, rookLeft, color) == 0) {
                    rookLeft = rookLeft >>> 1;
                }
                else {
                    rookLeft = 0;
                }
            }

            rookRight = rook << 1;
            while ((rookRight & rookMask) != 0 && rookRight != 0) {
                if (pieceAppend(rook, rookRight, color) == 0) {
                    rookRight = rookRight << 1;
                }
                else {
                    rookRight = 0;
                }
            }

            rookUp = rook << 8;
            while (rookUp != 0) {
                if (pieceAppend(rook, rookUp, color) == 0) {
                    rookUp = rookUp << 8;
                }
                else {
                    rookUp = 0;
                }
            }

            rookDown = rook >>> 8;
            while (rookDown != 0) {
                if (pieceAppend(rook, rookDown, color) == 0) {
                    rookDown = rookDown << 8;
                }
                else {
                    rookDown = 0;
                }
            }
        }

    }

    private void bishopAppend(long bishops, GamePiece.PieceColor color) {
        long bishopMask;
        long upperdiag;
        long lowerdiag;
        for (long bishop = bishops & -bishops; bishops != 0; bishops ^= bishop) {
            bishopMask = alternatingByteMask;
            if ((bishop & bishopMask) == 0) {
                bishopMask = ~alternatingByteMask;
            }

            upperdiag = bishop << 7;
            lowerdiag = bishop >>> 9;
            for (long leftSide = bishop >>> 1; (leftSide & bishopMask) != 0; leftSide = leftSide >>> 1) {
                if (upperdiag != 0) {
                    if (pieceAppend(bishop, upperdiag, color) == 0) {
                        upperdiag = upperdiag << 7;
                    }
                    else {
                        upperdiag = 0;
                    }
                }
                if (lowerdiag != 0) {
                    if (pieceAppend(bishop, lowerdiag, color) == 0) {
                        lowerdiag = lowerdiag >>> 9;
                    }
                    else {
                        lowerdiag = 0;
                    }
                }
            }

            upperdiag = bishop << 9;
            lowerdiag = bishop >>> 7;
            for (long rightSide = bishop >>> 1; (rightSide & bishopMask) != 0; rightSide = rightSide >>> 1) {
                if (upperdiag != 0) {
                    if (pieceAppend(bishop, upperdiag, color) == 0) {
                        upperdiag = upperdiag << 9; // Bishop hit an empty square, can continue on the diagonal.
                    }
                    else {
                        upperdiag = 0; // Bishop hit nonempty square, can no longer continue on the diagonal.
                    }
                }
                if (lowerdiag != 0) {
                    if (pieceAppend(bishop, lowerdiag, color) == 0) {
                        lowerdiag = lowerdiag >>> 7; // Bishop hit an empty square, can continue on the diagonal.
                    }
                    else {
                        lowerdiag = 0; // Bishop hit nonempty square, can no longer continue on the diagonal.
                    }
                }
            }
        }
    }

    private void knightAppend(long knights, GamePiece.PieceColor color) {
        long knightMask;
        for (long knight = knights & -knights; knights != 0; knights ^= knight) {
            if ((knight & alternatingByteMask) != 0) {
                knightMask = alternatingByteMask;
            }
            else {
                knightMask = ~alternatingByteMask;
            }
            if (((knight >>> 1) & knightMask) != 0) {
                if (((knight >>> 2) & knightMask) != 0) {
                    pieceAppend(knight, (knight << 8) >>> 2, color);
                    pieceAppend(knight, (knight >>> 8) >>> 2, color);
                }
                pieceAppend(knight, (knight << 16) >>> 1, color);
                pieceAppend(knight, (knight >>> 16) >>> 1, color);
            }

            if (((knight << 1) & knightMask) != 0) {
                if (((knight << 2) & knightMask) != 0) {
                    pieceAppend(knight, (knight << 8) << 2, color);
                    pieceAppend(knight, (knight >>> 8) << 2, color);
                }
                pieceAppend(knight, (knight << 16) >>> 1, color);
                pieceAppend(knight, (knight >>> 16) >>> 1, color);
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
     * @param pieceColor
     *      color of the piece that is moved
     * @return 0 if new location was an empty space
     *         1 if opponent piece captured at new location
     *        -1 if new location stored invoking player colored piece
     *        -2 if new location is off the board
     */
    private int pieceAppend(long oldLoc, long newLoc, GamePiece.PieceColor pieceColor) {
        // check to see if piece move is legal (no piece of same color) and append it whether it has captured
        // a black piece or not. Assumes the locations are valid and checked before function call.
        Square newSquare = bitToPos(newLoc);
        Square oldSquare = bitToPos(oldLoc);
        if (newLoc != 0) {
            // avoid the method call if the newLoc extends below or above the board.
            GamePiece captured = bitboard.getPiece(newSquare);
            if (captured != null) {
                if (pieceColor != captured.getColor()) {
                    possibleMoves.add(new Move(oldSquare, newSquare, captured.getType())); // opponent piece captured
                    return 1;
                }
                return -1;
            }
            else {
                possibleMoves.add(new Move(oldSquare, newSquare)); // empty square
                return 0;
            }
        }
        return -2;
    }


    private Square bitToPos(long bit) {
        int yPos = 0;
        while (bit > 0x80) {
            bit = bit >>> 8;
            yPos += 1;
        }

        int xPos = 0;
        while (bit > 0x1) {
            bit = bit >> 1;
            xPos += 1;
        }

        return new Square(xPos, yPos);
    }
}
