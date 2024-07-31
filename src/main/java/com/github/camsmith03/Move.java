package com.github.camsmith03;

/**
 * Will be used to determine all possible moves from a given Board.
 *
 */
public class Move {
    public enum CastleSide {QUEEN_SIDE, KING_SIDE, NONE};

    private long from;
    private long to;
    private Piece movedPiece;
    private Piece.Type capturedPiece;
    private Piece.Type promotedType;
    private long enPassantCapturedPiece = 0;
    private CastleSide castledRook;

    public Move(long fromMask, long toMask, Piece movedPiece) {
        this.from = fromMask;
        this.to = toMask;
        this.movedPiece = movedPiece;
        capturedPiece = Piece.Type.NONE;
        promotedType = Piece.Type.NONE;
        castledRook = CastleSide.NONE;
    }

    public Move(long fromMask, long toMask, Piece movedPiece, Piece.Type capturedPiece) {
        this.from = fromMask;
        this.to = toMask;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        promotedType = Piece.Type.NONE;
        castledRook = CastleSide.NONE;
    }

    public Move(long fromMask, long toMask, Piece movedPiece, Piece.Type capturedPiece, Piece.Type promotedType) {
        if (movedPiece.type != Piece.Type.PAWN)
            throw new IllegalArgumentException("Promoted piece must be a pawn");

        this.from = fromMask;
        this.to = toMask;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.promotedType = promotedType;
        castledRook = CastleSide.NONE;
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

    public Piece getMovedPiece() {
        return movedPiece;
    }

    public Piece.Type getMovedType() {
        return movedPiece.type;
    }

    public Piece.Color getMovedColor() {
        return movedPiece.color;
    }

    public Piece.Type getCapturedPieceType() {
        return capturedPiece;
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
            return capturedPiece == move.capturedPiece;
        }

        return false;
    }

    public Move rebuild(long fromMask, long toMask, Piece movedPiece, Piece.Type capturedPiece, Piece.Type promotedType) {
        this.from = fromMask;
        this.to = toMask;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.promotedType = promotedType;
        if (promotedType != Piece.Type.NONE && movedPiece.type != Piece.Type.PAWN) {
            throw new IllegalArgumentException("Promoted piece must be a pawn");
        }
        enPassantCapturedPiece = 0;
        castledRook = CastleSide.NONE;
        return this;
    }
}
