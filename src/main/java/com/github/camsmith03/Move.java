package com.github.camsmith03;

/**
 * Stores the fields required to identify every single possible move that can
 * exist on the chess boardController. Some moves (like e.p. or castling) require special
 * consideration by flipping certain flags.
 *
 * @author Cameron Smith
 * @version 08.08.2024
 */
public class Move implements Comparable<Move> {
    public enum CastleSide {QUEEN_SIDE, KING_SIDE, NONE};
    private final long from;
    private final long to;
    private final Piece.Type movedPieceType;
    private final Piece.Color movedPieceColor;
    private Piece.Type capturedPieceType;
    private Piece.Type promotedType;
    private long enPassantCapturedPiece = 0;
    private CastleSide castledRook;

    /**
     * Basic constructor for moves without capture.
     *
     * @param fromMask
     *      is a bit for the source square.
     * @param toMask
     *      is a bit for the destination square.
     * @param movedPieceType
     *      for the piece currently moving.
     * @param movedPieceColor
     *      for the piece currently moving.
     */
    public Move(long fromMask, long toMask, Piece.Type movedPieceType, Piece.Color movedPieceColor) {
        if (fromMask == 0 || toMask == 0)
            throw new IllegalAccessError("Improper move mask arguments");

        this.from = fromMask;
        this.to = toMask;
        this.movedPieceType = movedPieceType;
        this.movedPieceColor = movedPieceColor;
        capturedPieceType = Piece.Type.NONE;
        promotedType = Piece.Type.NONE;
        castledRook = CastleSide.NONE;
    }

    /**
     * Constructor for moves involving piece capture.
     *
     * @param fromMask
     *      is a bit for the source square.
     * @param toMask
     *      is a bit for the destination square.
     * @param movedPieceType
     *      for the piece currently moving.
     * @param movedPieceColor
     *      for the piece currently moving.
     * @param capturedPieceType
     *      for the piece captured at the toMask position.
     */
    public Move(long fromMask, long toMask, Piece.Type movedPieceType, Piece.Color movedPieceColor, Piece.Type capturedPieceType) {
        this(fromMask, toMask, movedPieceType, movedPieceColor);
        this.capturedPieceType = capturedPieceType;
    }

    /**
     * Constructor for moves involving pawn promotion.
     *
     * @param fromMask
     *      is a bit for the source square.
     * @param toMask
     *      is a bit for the destination square.
     * @param movedPieceType
     *      for the piece currently moving.
     * @param movedPieceColor
     *      for the piece currently moving.
     * @param capturedPieceType
     *      for the piece captured at the toMask position.
     * @param promotedType
     *      for the type being promoted to.
     */
    public Move(long fromMask, long toMask, Piece.Type movedPieceType, Piece.Color movedPieceColor, Piece.Type capturedPieceType, Piece.Type promotedType) {
        this(fromMask, toMask, movedPieceType, movedPieceColor, capturedPieceType);

        if (promotedType != Piece.Type.NONE && movedPieceType != Piece.Type.PAWN)
            throw new IllegalArgumentException("Promoted piece must be a pawn");

        this.promotedType = promotedType;
    }

    /**
     * Setter for castledRook.
     *
     * @param castledSide
     *      side being used in the castled (!= NONE)
     */
    public void setCastledRook(CastleSide castledSide) {
        if (castledSide == CastleSide.NONE)
            throw new IllegalArgumentException("Castle side cannot be NONE");

        castledRook = castledSide;
    }

    /**
     * Setter for the e.p. captured piece mask.
     *
     * @param capturedPieceMask
     *      mask/location of the captured piece.
     */
    public void setEnPassant(long capturedPieceMask) {
        enPassantCapturedPiece = capturedPieceMask;
    }

    /**
     * Getter for castledRook.
     *
     * @return castledRook
     */
    public CastleSide getCastledRook() {
        return castledRook;
    }

    /**
     * Getter for the e.p. captured piece mask (or location).
     *
     * @return enPassantCapturedPiece
     */
    public long getEnPassant() {
        return enPassantCapturedPiece;
    }

    /**
     * Getter for the source mask.
     *
     * @return from
     */
    public long getFromMask() {
        return from;
    }

    /**
     * Getter for the destination mask.
     *
     * @return to
     */
    public long getToMask() {
        return to;
    }

    /**
     * Getter for movedPieceType.
     *
     * @return movedPieceType
     */
    public Piece.Type getMovedPieceType() {
        return movedPieceType;
    }

    /**
     * Getter for movedPieceColor.
     *
     * @return movedPieceColor
     */
    public Piece.Color getMovedPieceColor() {
        return movedPieceColor;
    }

    /**
     * Getter for capturedPieceType.
     *
     * @return capturedPieceType
     */
    public Piece.Type getCapturedPieceType() {
        return capturedPieceType;
    }

    /**
     * Getter for promotedType.
     *
     * @return promotedType
     */
    public Piece.Type getPromotedType() {
        return promotedType;
    }

    /**
     * Basic toString() used for debugging and testing purposes.
     *
     * @return string representation of the move.
     */
    @Override
    public String toString() {
        return "Move [from=" + GameEngine.hexString(from) + ", to=" + GameEngine.hexString(to) + "]";
    }

    @Override
    public int compareTo(Move m) {
        return m.valueOf() - this.valueOf();
    }

    /**
     * Relative move value used for PriorityQueue Comparator. This is used in
     * the move ordering heuristic to keep the higher priority moves closer to
     * the front, thus leading their branches to be explored first.
     *
     * @return int representing the basic for the move
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
                case BISHOP:
                case ROOK:
                    value += 3;
                    break;
                default:
                    throw new IllegalStateException("promoted type is invalid");
            }
        }

        if (castledRook != CastleSide.NONE)
            value += 10;


        return switch (movedPieceType) {
            case PAWN -> value + 1;
            case KNIGHT -> value + 3;
            case BISHOP -> value + 2;
            default -> value;
        };
    }

}
