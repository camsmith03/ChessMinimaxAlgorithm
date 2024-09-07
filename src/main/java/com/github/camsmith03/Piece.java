package com.github.camsmith03;

/**
 * Very basic class holding the constants and public accessible fields to
 * represent the pieces for a chess board. In hindsight, a better design choice
 * instead of the NONE enum type might have been to leverage Java Optionals to
 * hold the type parameter.
 *
 * @author Cameron Smith
 * @version 07.30.2024
 */
public class Piece {
    public enum Type { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING, NONE }
    public enum Color { WHITE, BLACK }

    public final Type type;
    public final Color color;

    public Piece(Type type, Color color) {
        this.type = type;
        this.color = color;
    }
}
