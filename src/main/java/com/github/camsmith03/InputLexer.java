package com.github.camsmith03;

/**
 * InputLexer takes in the string input from a user query and parses each character. The input is expected to be a valid
 * form of chess notation as per the FIDE Rules Commission Standard. Once strings are parsed, relevant information is
 * extracted and passed to the InputAnalyzer, which will convert the data into a Move, and assure its validity for the
 * current board state.
 *
 * @author Cameron Smith
 * @version 08.08.2024
 */
public class InputLexer {
    private final InputAnalyzer analyzer;
    private final Board board;

    public InputLexer(Board board) {
        analyzer = new InputAnalyzer(board.getBitboard());
        this.board = board;
    }

    public Move interpret(String input) throws IllegalArgumentException, IllegalStateException {
        if (input == null || input.isEmpty() || input.length() == 1)
            throw new IllegalArgumentException();

        return interpretByType(input);
    }


    private Move interpretByType(String input) throws IllegalArgumentException, IllegalStateException {
        char piece = input.charAt(0);
        return switch (piece) {
            case 'N' -> parseNBRQ(Piece.Type.KNIGHT, input);
            case 'B' -> parseNBRQ(Piece.Type.BISHOP, input);
            case 'R' -> parseNBRQ(Piece.Type.ROOK,   input);
            case 'Q' -> parseNBRQ(Piece.Type.QUEEN,  input);
            case 'K' -> parseKing(input);
            case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h' -> parsePawn(input);
            default -> specialCases(input);
        };
    }


    private Move parsePawn(String input) throws IllegalArgumentException, IllegalStateException{
        char[] inputArr = new char[input.length()];
        input.getChars(0, input.length(), inputArr, 0);
        Piece.Color turnColor = board.getTurn();

        if (inputArr.length <= 8 && inputArr.length >= 2) {

            if (inputArr[1] == 'x') {
                if (inputArr.length >= 4 && isFile(inputArr[2]) && isRank(inputArr[3])) {
                    int destFile = getOffset(inputArr[2], true);
                    int destRank = getOffset(inputArr[3], false);
                    if (inputArr.length == 4) {
                        int sourceFile = getOffset(inputArr[0], true);
                        return analyzer.analyzePawnCapture(turnColor, destFile, destRank, sourceFile);
                    } else if (inputArr.length == 6) {
                        if (inputArr[4] == '=' && isPromotableTo(inputArr[5])) {
                            return analyzer.analyzePromotedPawn(input, turnColor, true, inputArr[5]);
                        }
                    } else if (inputArr.length == 8) {
                        if (input.startsWith("e.p.", 4)) {
                            return analyzer.analyzePawnEnPassant(input, turnColor);
                        }
                    }
                }
            } else if (isRank(inputArr[1])) {
                int destFile = getOffset(inputArr[0], true);
                int destRank = getOffset(inputArr[1], false);
                if (inputArr.length == 2) {
                    return analyzer.analyzePawn(turnColor, destFile, destRank);
                } else if (inputArr.length == 4) {
                    if (inputArr[2] == '=' && isPromotableTo(inputArr[3])) {
                        return analyzer.analyzePromotedPawn(input, turnColor, false, inputArr[3]);
                    }
                }
            }
        }


        throw new IllegalArgumentException("Invalid pawn notation: " + input);
    }

    // Knight, Bishop, Rook, or Queen
    private Move parseNBRQ(Piece.Type type, String input) throws IllegalArgumentException, IllegalStateException {
        if (input.length() < 3) throw new IllegalArgumentException("Invalid notation: " + input);


        char[] inputArr = new char[input.length()];
        input.getChars(0, input.length(), inputArr, 0);
        Piece.Color turnColor = board.getTurn();

        if (inputArr[1] == 'x') {
            if (inputArr.length == 4) {
                if (isFile(inputArr[2]) && isRank(inputArr[3])) {
                    int destFile = getOffset(inputArr[2], true);
                    int destRank = getOffset(inputArr[3], false);

                    return analyzer.analyzeNBRQ(turnColor, type, true, destFile, destRank);
                }
            }
        }
        else if (inputArr[2] == 'x') {
            if (inputArr.length == 5) {
                if (isFile(inputArr[3]) && isRank(inputArr[4])) {
                    int destFile = getOffset(inputArr[3], true);
                    int destRank = getOffset(inputArr[4], false);

                    if (isFile(inputArr[1])) {
                        int sourceFile = getOffset(inputArr[1], true);

                        return analyzer.analyzeNBRQ(turnColor, type, true, destFile, destRank, sourceFile, true);

                    } else if (isRank(inputArr[1])) {
                        int sourceRank = getOffset(inputArr[1], false);

                        return analyzer.analyzeNBRQ(turnColor, type, true, destFile, destRank, sourceRank, false);
                    }
                }
            }

        }
        else if (inputArr.length == 5) {
            if (isFile(inputArr[1]) && isRank(inputArr[2])) {
                if (isFile(inputArr[3]) && isRank(inputArr[4])) {
                    int destFile = getOffset(inputArr[3], true);
                    int destRank = getOffset(inputArr[4], false);
                    int sourceFile = getOffset(inputArr[1], true);
                    int sourceRank = getOffset(inputArr[2], false);

                    return analyzer.analyzeNBRQ(turnColor, type, false, destFile, destRank, sourceFile, sourceRank);
                }
            }
        }
        else if (inputArr.length == 6 && inputArr[3] == 'x') {

            if (isFile(inputArr[1]) && isRank(inputArr[2])) {
                if (isFile(inputArr[4]) && isRank(inputArr[5])) {
                    int sourceFile = getOffset(inputArr[1], true);
                    int sourceRank = getOffset(inputArr[2], false);
                    int destFile = getOffset(inputArr[4], true);
                    int destRank = getOffset(inputArr[5], false);

                    return analyzer.analyzeNBRQ(turnColor, type, true, destFile, destRank, sourceFile, sourceRank);

                }
            }
        }
        else {
            if (inputArr.length == 3) {
                if (isFile(inputArr[1]) && isRank(inputArr[2])) {
                    int destFile = getOffset(inputArr[1], true);
                    int destRank = getOffset(inputArr[2], false);

                    return analyzer.analyzeNBRQ(turnColor, type, false, destFile, destRank);
                }
            }
            else if (inputArr.length == 4) {
                if (isFile(inputArr[2]) && isRank(inputArr[3])) {
                    int destFile = getOffset(inputArr[2], true);
                    int destRank = getOffset(inputArr[3], false);

                    if (isFile(inputArr[1])) {
                        int sourceFile = getOffset(inputArr[1], true);
                        return analyzer.analyzeNBRQ(turnColor, type, false, destFile, destRank, sourceFile, true);

                    } else if (isRank(inputArr[1])) {
                        int sourceRank = getOffset(inputArr[1], false);
                        return analyzer.analyzeNBRQ(turnColor, type, false, destFile, destRank, sourceRank, false);
                    }
                }
            }
        }

        throw new IllegalArgumentException("Invalid notation: " + input);
    }

    private Move parseKing(String input) throws IllegalArgumentException, IllegalStateException {
        Piece.Color turnColor = board.getTurn();
        if (input.length() == 3 && isFile(input.charAt(1)) && isRank(input.charAt(2))) {
            int destFile = getOffset(input.charAt(1), true);
            int destRank = getOffset(input.charAt(2), false);
            return analyzer.analyzeKingMove(turnColor, destFile, destRank);
        }

        throw new IllegalArgumentException("Invalid king notation: " + input);
    }

    private Move specialCases(String input) throws IllegalArgumentException, IllegalStateException {
        if (input.equals("0-0")) {
            return analyzer.analyzeCastle(true, board.getTurn());
        }
        else if (input.equals("0-0-0")) {
            return analyzer.analyzeCastle(false, board.getTurn());
        }

        // TODO: implement rest of unlikely edge conditions for FIDE notation
        throw new IllegalArgumentException("Unknown input notation: " + input);
    }

    private boolean isRank(char c) {
        return (c - 48) >= 1 && (c - 48) <= 8;
    }

    private boolean isFile(char c) {
        return (c - 97) >= 0 && (c - 97) <= 7;
    }

    private int getOffset(char c, boolean isFile) {
        return isFile ? (c - 97) : (c - 48) - 1; // offset zero based (rank - 1)
    }

    private boolean isPromotableTo(char c) {
        return switch (c) { case 'Q', 'B', 'R', 'N' -> true; default -> false; };
    }

}

//    private Move interpretPawn(String input, Board board) throws IllegalStateException {
//        // FIXME: Consider if pawn of same color occupies square above.
//        if (input.charAt(1) == 'x') {
//            // piece is captured
//            long captured;
//            if (input.length() == 4) {
//                captured = getMask(input.charAt(2), getAsciiInt(input.charAt(3)));
//            }
//            else {
//                throw new IllegalStateException("Invalid pawn capture notation");
//            }
//
//            Piece.Color color = board.getTurn();
//
//            int rank;
//            if (color == Piece.Color.WHITE) {
//                rank = getAsciiInt(input.charAt(3)) - 1;
//            }
//            else {
//                rank = getAsciiInt(input.charAt(3)) + 1;
//            }
//
//            long pawnFrom = getMask(input.charAt(0), rank);
//            Piece pawnLoc = board.getPiece(pawnFrom);
//
//            if (pawnLoc == null) {
//                throw new IllegalStateException("Invalid pawn start position");
//            }
//
//            if ( pawnLoc.color != color ) {
//                throw new IllegalStateException("Invalid pawn start location");
//            }
//
//            Piece capturedPiece = board.getPiece(captured);
//
//            if (capturedPiece == null) {
//                // check for en passant
//                long passantPiece;
//                if (color == Piece.Color.WHITE) {
//                    passantPiece = captured >>> 8;
//                }
//                else {
//                    passantPiece = captured << 8;
//                }
//                capturedPiece = board.getPiece(passantPiece);
//
//                if (capturedPiece != null) {
//                    // we now know an enemy piece exists in a potential en passant position we can leverage.
//
//                    // Check the enPassantBoard to ensure the piece in question has been marked from the previous turn.
//                    long enPassantBoard = board.getBitboard().enPassantBoard;
//                    if ((enPassantBoard & passantPiece) == 0) {
//                        throw new IllegalStateException("Invalid en passant captured location");
//                    }
//                    // At this point, we can confirm the piece has been captured and the move was successful.
//                    Move enPassantMove = new Move(pawnFrom, captured, Piece.Type.PAWN, color, Piece.Type.PAWN);
//
//                    // We have to set the enPassantCaptured bit in the Move object to signify that there was a capture
//                    // without having the piece be assumed to exist at the "pawnTo" location.
//                    enPassantMove.setEnPassant(passantPiece);
//
//                    return enPassantMove;
//                }
//                else {
//                    throw new IllegalStateException("Invalid en passant captured location");
//                }
//            }
//
//            if (capturedPiece.color == color) {
//                throw new IllegalStateException("Invalid pawn capture notation");
//            }
//
//            return new Move(pawnFrom, captured, Piece.Type.PAWN, color, capturedPiece.type);
//
//        }
//        else {
//            // piece is not captured
//            if (input.length() != 2) {
//                throw new IllegalStateException("Invalid pawn move notation");
//            }
//
//            Piece.Color turn = board.getTurn();
//            long pawnTo = getMask(input.charAt(0), getAsciiInt(input.charAt(1)));
//            long pawnFrom;
//            Piece pawn;
//
//            if (turn == Piece.Color.WHITE) {
//                if ((pawnTo & 0x00000000FF000000L) != 0) {
//                    // pawn can move two up legally, must check for that
//                    Piece pawn1 = board.getPiece(pawnTo >>> 8);
//                    Piece pawn2 = board.getPiece(pawnTo >>> 16);
//                    if (pawn1 == null) {
//                        if (pawn2 == null) {
//                            throw new IllegalStateException("Invalid pawn move notation");
//                        }
//                        pawn = pawn2;
//                        pawnFrom = pawnTo >>> 16;
//                    }
//                    else {
//                        pawn = pawn1;
//                        pawnFrom = pawnTo >>> 8;
//                    }
//                }
//                else {
//                    pawn = board.getPiece(pawnTo >>> 8);
//                    pawnFrom = pawnTo >>> 8;
//                    if (pawn == null) {
//                        throw new IllegalStateException("Invalid pawn move notation");
//                    }
//                }
//            }
//            else {
//                if ((pawnTo & 0x000000FF00000000L) != 0) {
//                    Piece pawn1 = board.getPiece(pawnTo << 8);
//                    Piece pawn2 = board.getPiece(pawnTo << 16);
//
//                    if (pawn1 == null) {
//                        if (pawn2 == null) {
//                            throw new IllegalStateException("Invalid pawn move notation");
//                        }
//                        pawn = pawn2;
//                        pawnFrom = pawnTo << 16;
//                    }
//                    else {
//                        pawn = pawn1;
//                        pawnFrom = pawnTo << 8;
//                    }
//                }
//                else {
//                    pawn = board.getPiece(pawnTo << 8);
//                    pawnFrom = pawnTo << 8;
//                    if (pawn == null) {
//                        throw new IllegalStateException("Invalid pawn move notation");
//                    }
//                }
//            }
//
//            return new Move(pawnFrom, pawnTo, Piece.Type.PAWN, turn);
//        }
//    }

//    private Move interpretKnight(String input, Board board) throws IllegalStateException {
//        Piece.Color color = board.getTurn();
//        long destKnight = getMask(input.charAt(input.length() - 2), getAsciiInt(input.charAt(input.length() - 1)));
//        long colorKnights = board.getBitboard().getColorBoards(color)[1];
//        long gameBoard = board.getBitboard().getGameBoard();
//        long oppBoard;
//        if (color == Piece.Color.WHITE) {
//            oppBoard = board.getBitboard().getBoardColor(Piece.Color.BLACK);
//        }
//        else {
//            oppBoard = board.getBitboard().getBoardColor(Piece.Color.WHITE);
//        }
//
//
//        if (input.length() == 3) {
//            // normal masking procedure (source square unknown)
//            long knightMask = maskGenerator(destKnight, Piece.Type.KNIGHT);
//            long sourceKnight = colorKnights & knightMask;
//
//            if (sourceKnight != 0 && (gameBoard & destKnight) == 0) {
//                // make sure that a knight is at the source, and no piece was in the destination square
//                return new Move(sourceKnight, destKnight, Piece.Type.KNIGHT, color);
//            }
//        }
//        else if (input.length() == 4) {
//            int rankOrFile = getAsciiInt(input.charAt(1));
//
//            if (input.charAt(1) == 'x') {
//                // capture with normal masking
//                long knightMask = maskGenerator(destKnight, Piece.Type.KNIGHT);
//                long sourceKnight = colorKnights & knightMask;
//
//                if (sourceKnight != 0 && (oppBoard & destKnight) != 0) {
//                    // ensure that a knight is at the source, and the opponents board has a piece that is captured.
//                    return new Move(sourceKnight, destKnight, Piece.Type.KNIGHT, color, board.getPiece(destKnight).type);
//                }
//            }
//            else if (rankOrFile < 57 && rankOrFile  > 48) {
//                // two knights from the mask with duplicate files, rankOrFile has the rank of the moved knight
//                int offset = rankOrFile - 48;
//                long rank01 = 0x00000000000000FFL;
//                long trueRank = rank01 << (offset * 8);
//
//                long knightMask = maskGenerator(destKnight, Piece.Type.KNIGHT);
//                long sourceKnight = colorKnights & (knightMask & trueRank);
//
//                if (sourceKnight != 0 && (gameBoard & destKnight) == 0) {
//                    // ensure a knight exists at the provided rank, and no piece exists on the moved to square.
//                    return new Move(sourceKnight, destKnight, Piece.Type.KNIGHT, color);
//                }
//
//            }
//            else if (rankOrFile < 105 && rankOrFile  > 97) {
//                // two knights from the mask can be starting square. Use file of true knight to disambiguate
//                int offset = rankOrFile - 97;
//                long fileA = 0x0101010101010101L;
//                long trueFile = fileA << offset;
//
//                long knightMask = maskGenerator(destKnight, Piece.Type.KNIGHT);
//                long sourceKnight = colorKnights & (knightMask & trueFile);
//
//                if (sourceKnight != 0 && (gameBoard & destKnight) == 0) {
//                    // ensure a knight exists at the file specified, and ensure that no piece exists at the destination
//                    // square of interest.
//                    return new Move(sourceKnight, destKnight, Piece.Type.KNIGHT, color);
//                }
//            }
//        }
//        else if (input.length() == 5 && input.charAt(2) == 'x') {
//            // Can't disambiguate between two knights for a captured piece turn.
//            int rankOrFile = getAsciiInt(input.charAt(1));
//
//            if (rankOrFile < 57 && rankOrFile  > 48) {
//                // two knights from the mask with duplicate files, rankOrFile has the rank of the moved knight
//                int offset = rankOrFile - 48;
//                long rank01 = 0x00000000000000FFL;
//                long trueRank = rank01 << (offset * 8);
//
//                long knightMask = maskGenerator(destKnight, Piece.Type.KNIGHT);
//                long sourceKnight = colorKnights & (knightMask & trueRank);
//
//                if (sourceKnight != 0 && (oppBoard & destKnight) != 0) {
//                    // ensure a knight exists at the provided rank, and an enemy piece exists on the moved to square.
//                    return new Move(sourceKnight, destKnight, Piece.Type.KNIGHT, color, board.getPiece(destKnight).type);
//                }
//            }
//            else if (rankOrFile < 105 && rankOrFile  > 97) {
//                // two knights from the mask can be starting square. Use file of true knight to disambiguate
//                int offset = rankOrFile - 97;
//                long fileA = 0x0101010101010101L;
//                long trueFile = fileA << offset;
//
//                long knightMask = maskGenerator(destKnight, Piece.Type.KNIGHT);
//                long sourceKnight = colorKnights & (knightMask & trueFile);
//
//                if (sourceKnight != 0 && (oppBoard & destKnight) != 0) {
//                    // ensure a knight exists at the file specified, and ensure that an enemy piece exists at the
//                    // destination square of interest.
//                    return new Move(sourceKnight, destKnight, Piece.Type.KNIGHT, color, board.getPiece(destKnight).type);
//                }
//            }
//        }
//
//        throw new IllegalStateException("Illegal Knight Move Notation");
//    }

// Knight, Bishop, or Rook
//    private Move parseNBRQ(Piece.Type type, String input) throws IllegalArgumentException, IllegalStateException {
//        if (input.length() < 3) throw new IllegalArgumentException("Invalid notation: " + input);
//
//        Piece.Color turnColor = board.getTurn();
//        char[] inputArr = new char[input.length()];
//        input.getChars(0, input.length(), inputArr, 0);
//
//
//        if (inputArr[1] == 'x') {
//            if (inputArr.length == 4) {
//                if (isFile(inputArr[2]) && isRank(inputArr[3])) {
//                    int destFile = getOffset(inputArr[2], true);
//                    int destRank = getOffset(inputArr[3], false);
//
//                    return analyzer.analyzeNBR(turnColor, type, true, destFile, destRank);
//                }
//            }
//        }
//        else if (inputArr[2] == 'x') {
//            if (inputArr.length == 5) {
//                if (isFile(inputArr[3]) && isRank(inputArr[4])) {
//                    int destFile = getOffset(inputArr[3], true);
//                    int destRank = getOffset(inputArr[4], false);
//
//                    if (isFile(inputArr[1])) {
//                        int sourceFile = getOffset(inputArr[1], true);
//                        return analyzer.analyzeNBR(turnColor, type, true, destFile, destRank, sourceFile, true);
//
//                    } else if (isRank(inputArr[1])) {
//                        int sourceRank = getOffset(inputArr[1], false);
//                        return analyzer.analyzeNBR(turnColor, type, true, destFile, destRank, sourceRank, false);
//                    }
//                }
//            }
//        }
//        else {
//            if (inputArr.length == 3) {
//                if (isFile(inputArr[1]) && isRank(inputArr[2])) {
//                    int destFile = getOffset(inputArr[1], true);
//                    int destRank = getOffset(inputArr[2], false);
//                    return analyzer.analyzeNBR(turnColor, type, false, destFile, destRank);
//
//                }
//            }
//            else if (inputArr.length == 4) {
//                if (isFile(inputArr[2]) && isRank(inputArr[3])) {
//                    int destFile = getOffset(inputArr[2], true);
//                    int destRank = getOffset(inputArr[3], false);
//
//                    if (isFile(inputArr[1])) {
//                        int sourceFile = getOffset(inputArr[1], true);
//                        return analyzer.analyzeNBR(turnColor, type, false, destFile, destRank, sourceFile, true);
//
//                    } else if (isRank(inputArr[1])) {
//                        int sourceRank = getOffset(inputArr[1], false);
//                        return analyzer.analyzeNBR(turnColor, type, false, destFile, destRank, sourceRank, false);
//                    }
//                }
//            }
//        }
//
//        throw new IllegalArgumentException("Invalid notation: " + input);
//    }