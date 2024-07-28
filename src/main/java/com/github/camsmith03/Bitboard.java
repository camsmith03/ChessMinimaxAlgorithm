package com.github.camsmith03;

/**
 * <p>
 * Bitboard implemented using longs that represent a 64 square chess board (1 for occupied, 0 for empty). Highly more
 * efficient than other approaches as far less space is consumed by the game board.
 * </p>
 * <p>
 * Follows standard Bitboard conventions:
 * <ul><li>LSB: Square a1 => Square (0, 0)</li><li>MSB: Square h8 => Square (7, 7) </li></ul>
 * </p>
 * <p>
 * Bitboard is not concerned over chess move legality, as that will be handled by the Board class. However, it will
 * ensure the bounds provided do not overflow the integer.
 * to take a move, translate it using bit manipulation, then update the boards accordingly.
 * </p>
 *
 * @author Cameron Smith
 * @version 07.26.2024
 */
public class Bitboard {
    private final long[] whiteBoards = new long[6];
    private final long[] blackBoards = new long[6];
    private long gameBoard;
    private final GamePiece.PieceType[] pieceTypeArr;

    public Bitboard() {
        // White pieces corresponding to long bitboards
        whiteBoards[0] = 0xFF00; // White Pawns
        whiteBoards[1] = 0x0042; // White Knights
        whiteBoards[2] = 0x0024; // White Bishops
        whiteBoards[3] = 0x0081; // White Rooks
        whiteBoards[4] = 0x0010; // White Queen
        whiteBoards[5] = 0x0080; // White King

        // Black pieces corresponding to long bitboards
        blackBoards[0] = 0x00FF000000000000L; // Black Pawns
        blackBoards[1] = 0x4200000000000000L; // Black Knights
        blackBoards[2] = 0x2400000000000000L; // Black Bishops
        blackBoards[3] = 0x8100000000000000L; // Black Rooks
        blackBoards[4] = 0x1000000000000000L; // Black Queen
        blackBoards[5] = 0x0800000000000000L; // Black King

        for (int i = 0; i < 6; i++) {
            gameBoard = whiteBoards[i] | blackBoards[i];
        }

        pieceTypeArr = GamePiece.PieceType.values();
    }

    /**
     * Moves a GamePiece from an initial position to a final position. Does not check for move legality. This will lead
     * to unexpected behavior without control over legal/illegal moves.
     *
     * @param piece
     *      Piece expected to be moved
     * @param from
     *      Square to move from
     * @param to
     *      Square to move to
     */
    public void movePiece(GamePiece piece, Square from, Square to) {
        int boardIndex = piece.getType().ordinal();
        long[] board;

        if (piece.getColor() == GamePiece.PieceColor.WHITE) {
            board = whiteBoards;
        }
        else {
            board = blackBoards;
        }


        long fromMask = posToMask(from);
        long toMask = posToMask(to);

        board[boardIndex] ^= fromMask | toMask;
        gameBoard ^= boardIndex;
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
     * @param pos
     *      Square to check for a piece's location of.
     * @return GamePiece if exists, null otherwise
     */
    public GamePiece getPiece(Square pos) {
        long mask = posToMask(pos);
        if ((gameBoard & mask) != 0) {
            GamePiece.PieceColor color;
            long[] pieceBitboards;

            if ((getWhiteBitboard() & mask) != 0) {
                color = GamePiece.PieceColor.WHITE;
                pieceBitboards = whiteBoards;
            }
            else {
                color = GamePiece.PieceColor.BLACK;
                pieceBitboards = blackBoards;
            }

            for (int i = 0; i < 6; i++) {
                if ((pieceBitboards[i] & mask) != 0) {
                    return new GamePiece(pieceTypeArr[i], color); // piece type same as bitboard index
                }
            }

            throw new IllegalStateException(); // thrown in any impossible states that would otherwise be ignored by
                                               // null return
        }

        return null; // no piece at that position
    }

    /**
     * Returns a long containing only the white pieces as bits.
     *
     * @return long
     */
    public long getWhiteBitboard() {
        long whiteBitboard = 0;
        for (int i = 0; i < 6; i++) {
            whiteBitboard = whiteBitboard | whiteBoards[i];
        }
        return whiteBitboard;
    }

    /**
     * Returns a long containing only the black pieces as bits.
     *
     * @return long
     */
    public long getBlackBitboard() {
        return gameBoard ^ getWhiteBitboard();
    }

    public long[] getWhitePieceBitboards() {
        return whiteBoards;
    }

    public long[] getBlackPieceBitboards() {
        return blackBoards;
    }

    public long getGameBoard() {
        return gameBoard;
    }
}
