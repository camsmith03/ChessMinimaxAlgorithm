package com.github.camsmith03;

/**
 * Will be used to determine all possible moves from a given Board.
 *
 */
public class Move {
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

        if (movedPieceType != Piece.Type.PAWN)
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
        return "Move [from=" + from + ", to=" + to + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;

        if (from == move.from && to == move.to) {
            return capturedPieceType == move.capturedPieceType;
        }

        return false;
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
}
