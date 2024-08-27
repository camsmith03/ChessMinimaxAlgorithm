package com.github.camsmith03;

/**
 * Will be used to determine all possible moves from a given Board.
 *
 */
public class Move implements Comparable<Move> {


    public enum CastleSide {QUEEN_SIDE, KING_SIDE, NONE};
    private long from;
    private long to;
    private Piece.Type movedPieceType;
    private Piece.Color movedPieceColor;
    private Piece.Type capturedPieceType;
    private Piece.Type promotedType;
    private long enPassantCapturedPiece = 0;
    private CastleSide castledRook;

    public Move(long fromMask, long toMask, Piece.Type movedPieceType, Piece.Color movedPieceColor) {
        if (fromMask == 0 || toMask == 0) {
            throw new IllegalAccessError("Improper move mask arguments");
        }
        this.from = fromMask;
        this.to = toMask;
        this.movedPieceType = movedPieceType;
        this.movedPieceColor = movedPieceColor;
        capturedPieceType = Piece.Type.NONE;
        promotedType = Piece.Type.NONE;
        castledRook = CastleSide.NONE;
    }

    public Move(long fromMask, long toMask, Piece.Type movedPieceType, Piece.Color movedPieceColor, Piece.Type capturedPieceType) {
        this(fromMask, toMask, movedPieceType, movedPieceColor);
        this.capturedPieceType = capturedPieceType;
    }

    public Move(long fromMask, long toMask, Piece.Type movedPieceType, Piece.Color movedPieceColor, Piece.Type capturedPiece, Piece.Type promotedType) {
        this(fromMask, toMask, movedPieceType, movedPieceColor, capturedPiece);

        if (promotedType != Piece.Type.NONE && movedPieceType != Piece.Type.PAWN)
            throw new IllegalArgumentException("Promoted piece must be a pawn");

        this.promotedType = promotedType;
    }

    public CastleSide getCastledRook() {
        return castledRook;
    }

    public void setCastledRook(CastleSide castledSide) {
        castledRook = castledSide;
    }

    public void setEnPassant(long capturedPieceMask) {
        enPassantCapturedPiece = capturedPieceMask;
    }

    public long getEnPassant() {
        return enPassantCapturedPiece;
    }

    public long getFromMask() {
        return from;
    }

    public long getToMask() {
        return to;
    }

    public Piece.Type getMovedPieceType() {
        return movedPieceType;
    }

    public Piece.Color getMovedPieceColor() {
        return movedPieceColor;
    }

    public Piece.Type getCapturedPieceType() {
        return capturedPieceType;
    }

    public Piece.Type getPromotedType() {
        return promotedType;
    }

    @Override
    public String toString() {
        return "Move [from=" + GameEngine.hexString(from) + ", to=" + GameEngine.hexString(to) + "]";
    }

    public Move rebuild(long fromMask, long toMask, Piece.Type movedPieceType, Piece.Color movedPieceColor, Piece.Type capturedPieceType, Piece.Type promotedType) {
        this.from = fromMask;
        this.to = toMask;
        this.movedPieceType = movedPieceType;
        this.movedPieceColor = movedPieceColor;
        this.capturedPieceType = capturedPieceType;
        this.promotedType = promotedType;
        if (promotedType != Piece.Type.NONE && movedPieceType != Piece.Type.PAWN) {
            throw new IllegalArgumentException("Promoted piece must be a pawn");
        }
        enPassantCapturedPiece = 0;
        castledRook = CastleSide.NONE;
        return this;
    }

    @Override
    public int compareTo(Move m) {
        return m.valueOf() - this.valueOf();
    }

    /**
     * Relative piece value used for PriorityQueue Comparator.
     *
     * @return
     */
    private int valueOf() {
        int value = 0;
        if (capturedPieceType != Piece.Type.NONE) {
            switch (capturedPieceType) {
                case KING:
                    return Integer.MAX_VALUE;
                case QUEEN:
                    value = 9;
                    break;
                case ROOK:
                    value = 5;
                    break;
                case BISHOP:
                case KNIGHT:
                    value = 3;
                    break;
                default:
                    value = 1;
            }
        }

        if (promotedType != Piece.Type.NONE) {
            switch (promotedType) {
                case QUEEN:
                    value += 20;
                    break;
                case KNIGHT:
                    value += 3;
                    break;
                default:
                    value = 0;
            }
        }

        if (castledRook != CastleSide.NONE) {
            value += 10;
        }

        return switch (movedPieceType) {
            case PAWN -> value + 1;
            case KNIGHT -> value + 3;
            case BISHOP -> value + 2;
            default -> value;
        };
    }

}
