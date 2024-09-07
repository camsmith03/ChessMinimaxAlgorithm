package com.github.camsmith03;

/**
 * InputLexer takes in the string input from a user query and parses each character. The input is expected to be a valid
 * form of chess notation as per the FIDE Rules Commission Standard. Once strings are parsed, relevant information is
 * extracted and passed to the InputAnalyzer, which will convert the data into a Move, and assure its validity for the
 * current boardController state.
 *
 * @author Cameron Smith
 * @version 08.08.2024
 */
public class InputLexer {
    private final InputAnalyzer analyzer;
    private final BoardController boardController;

    /**
     * Constructor to create append the boardController field and pass the bitboard to the
     * input analyzer.
     *
     * @param boardController
     *      Starting chess boardController.
     */
    public InputLexer(BoardController boardController) {
        analyzer = new InputAnalyzer(boardController.getBitboard());
        this.boardController = boardController;
    }

    /**
     * Passing a string input, it will check the grammar legality, and make the
     * necessary calls to the analyzer to check the move's legality. Once both
     * have been established, it will create a Move object, and return it to the
     * user. This manages to hide a significant amount of the complexity behind
     * a single, straight forward method call. Note: the input is CASE-SENSITIVE
     * as that is expected under the FIDE grammar rules.
     *
     * @param input
     *      String input to analyze
     * @return Move
     *      Corresponds to the input stream if it is considered valid.
     *
     * @throws IllegalArgumentException
     *      Thrown if there was a lexical issue detected (i.e.: the input isn't
     *      composed of any legal FIDE notation).
     * @throws IllegalStateException
     *      Thrown if there was an analytical issue detected (i.e.: the notation
     *      was valid, but the move can't be made on the boardController).
     */
    public Move interpret(String input) throws IllegalArgumentException, IllegalStateException {
        if (input == null || input.isEmpty() || input.length() == 1)
            throw new IllegalArgumentException();

        return interpretByType(input);
    }

    /**
     * Takes a string input, views the first character, and determines which
     * piece type is moved. Then, the proper method is called that will handle
     * the further lexical analysis.
     *
     * @param input
     *      String move that was made.
     * @return Move
     *      if deemed legal.
     *
     * @throws IllegalArgumentException
     *      Thrown if there was a lexical issue detected (i.e.: the input isn't
     *      composed of any legal FIDE notation).
     * @throws IllegalStateException
     *      Thrown if there was an analytical issue detected (i.e.: the notation
     *      was valid, but the move can't be made on the boardController).
     */
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


    /**
     * Since the type can be inferred as a pawn, grammar rules that are specific
     * to pawns will be used to check for legality. If it lexically looks sound,
     * The known rank and files will be parsed, converted to integers, and
     * passed to the input analyzer for further review.
     *
     * @param input
     *      String pawn move notation to parse.
     * @return Move
     *      if deemed legal.
     *
     * @throws IllegalArgumentException
     *      Thrown if there was a lexical issue detected (i.e.: the input isn't
     *      composed of any legal FIDE notation).
     * @throws IllegalStateException
     *      Thrown if there was an analytical issue detected (i.e.: the notation
     *      was valid, but the move can't be made on the boardController).
     */
    private Move parsePawn(String input) throws IllegalArgumentException, IllegalStateException{
        char[] inputArr = new char[input.length()];
        input.getChars(0, input.length(), inputArr, 0);
        Piece.Color turnColor = boardController.getTurn();

        if (inputArr.length <= 8 && inputArr.length >= 2) {

            if (inputArr[1] == 'x') {
                if (inputArr.length >= 4 && isFile(inputArr[2]) && isRank(inputArr[3])) {
                    int destFile = getOffset(inputArr[2], true);
                    int destRank = getOffset(inputArr[3], false);
                    if (inputArr.length == 4) {
                        int sourceFile = getOffset(inputArr[0], true);
                        return analyzer.analyzePawnCapture(turnColor, destFile, destRank, sourceFile);
                    } else if (inputArr.length == 6) {
                        if (inputArr[4] == '=' && isPromotableTo(inputArr[5]))
                            return analyzer.analyzePromotedPawn(input, turnColor, true, inputArr[5]);
                    } else if (inputArr.length == 8) {
                        if (input.startsWith("e.p.", 4))
                            return analyzer.analyzePawnEnPassant(input, turnColor);
                    }
                }
            }
            else if (isRank(inputArr[1])) {
                int destFile = getOffset(inputArr[0], true);
                int destRank = getOffset(inputArr[1], false);
                if (inputArr.length == 2)
                    return analyzer.analyzePawn(turnColor, destFile, destRank);
                else if (inputArr.length == 4)
                    if (inputArr[2] == '=' && isPromotableTo(inputArr[3]))
                        return analyzer.analyzePromotedPawn(input, turnColor, false, inputArr[3]);
            }
        }

        throw new IllegalArgumentException("Invalid pawn notation: " + input);
    }


    /**
     * It can be observed that the Knight, Bishop, Rook, and Queen all share a
     * notation that only grammatically differs in the initial character. Thus,
     * it made sense to combine the parsing into one method that would handle
     * all the grammar rules to observe, without concern over type.
     *
     * @param type
     *      Corresponding to the first letter observed in the interpretByType
     *      method (either Knight(N), Bishop(B), Rook(R), or Queen(Q)).
     * @param input
     *      String notation for the N/B/R/Q move that was made.
     * @return Move
     *      if deemed legal.
     *
     * @throws IllegalArgumentException
     *      Thrown if there was a lexical issue detected (i.e.: the input isn't
     *      composed of any legal FIDE notation).
     * @throws IllegalStateException
     *      Thrown if there was an analytical issue detected (i.e.: the notation
     *      was valid, but the move can't be made on the boardController).
     */
    private Move parseNBRQ(Piece.Type type, String input) throws IllegalArgumentException, IllegalStateException {
        if (input.length() < 3) throw new IllegalArgumentException("Invalid notation: " + input);

        char[] inputArr = new char[input.length()];
        input.getChars(0, input.length(), inputArr, 0);
        Piece.Color turnColor = boardController.getTurn();

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
                    }
                    else if (isRank(inputArr[1])) {
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

                    }
                    else if (isRank(inputArr[1])) {
                        int sourceRank = getOffset(inputArr[1], false);
                        return analyzer.analyzeNBRQ(turnColor, type, false, destFile, destRank, sourceRank, false);
                    }
                }
            }
        }

        throw new IllegalArgumentException("Invalid notation: " + input);
    }

    /**
     * Method to parse moves made by the king. This does not include the castle
     * move, as that is handled under the specialCases method.
     *
     * @param input
     *      String king move in chess notation.
     * @return Move
     *      if deemed legal.
     *
     * @throws IllegalArgumentException
     *      Thrown if there was a lexical issue detected (i.e.: the input isn't
     *      composed of any legal FIDE notation).
     * @throws IllegalStateException
     *      Thrown if there was an analytical issue detected (i.e.: the notation
     *      was valid, but the move can't be made on the boardController).
     */
    private Move parseKing(String input) throws IllegalArgumentException, IllegalStateException {
        Piece.Color turnColor = boardController.getTurn();
        if (input.length() == 3 && isFile(input.charAt(1)) && isRank(input.charAt(2))) {
            int destFile = getOffset(input.charAt(1), true);
            int destRank = getOffset(input.charAt(2), false);

            return analyzer.analyzeKingMove(turnColor, destFile, destRank);
        }

        throw new IllegalArgumentException("Invalid king notation: " + input);
    }

    /**
     * Method to default to if the type can't be inferred. This will handle any
     * edge cases that are still considered legal, while throwing an exception
     * for any cases that don't match the grammar.
     *
     * @param input
     *      String input in FIDE notation.
     * @return Move
     *      if deemed legal.
     *
     * @throws IllegalArgumentException
     *      Thrown if there was a lexical issue detected (i.e.: the input isn't
     *      composed of any legal FIDE notation).
     * @throws IllegalStateException
     *      Thrown if there was an analytical issue detected (i.e.: the notation
     *      was valid, but the move can't be made on the boardController).
     */
    private Move specialCases(String input) throws IllegalArgumentException, IllegalStateException {
        if (input.equals("0-0")) {
            return analyzer.analyzeCastle(true, boardController.getTurn());
        }
        else if (input.equals("0-0-0")) {
            return analyzer.analyzeCastle(false, boardController.getTurn());
        }

        // TODO: implement rest of unlikely edge conditions for FIDE notation
        throw new IllegalArgumentException("Unknown input notation: " + input);
    }

    /**
     * When given a single character, this will determine if it is a number from
     * 1 to 8, corresponding to the 8 ranks found on a chess boardController.
     *
     * @param c
     *      character representing possible rank.
     * @return true if it's a number between 1 and 8; false otherwise.
     */
    private boolean isRank(char c) {
        return (c - 48) >= 1 && (c - 48) <= 8;
    }

    /**
     * When given a single character, this will determine if it is a letter from
     * a through h, corresponding to the files found on a chess boardController.
     *
     * @param c
     *      character representing possible file.
     * @return true if it's a letter from a to h; false otherwise.
     */
    private boolean isFile(char c) {
        return (c - 97) >= 0 && (c - 97) <= 7;
    }

    /**
     * Simply put, when provided a character and a boolean representing if the
     * character is a file, it will implicitly cast it to an integer,
     * subtracting by its ASCII offset, and return the index it would represent
     * which is passed to the analyzer. Also, the rank is zeroed out to make
     * array accesses easier.
     *
     * @param c
     *      Character representing a file or a rank.
     * @param isFile
     *      flag used to indicate which conversion should return. If false, even
     *      if the input isn't a rank, it will be treated like one.
     * @return int
     *      corresponding to the proper index of the character's type.
     */
    private int getOffset(char c, boolean isFile) {
        return isFile ? (c - 97) : (c - 48) - 1; // offset zero based (rank - 1)
    }

    /**
     * Used exclusively by the pawn analysis to determine if the piece an input
     * designates as it's promotion is one of the four legal pieces to promote
     * to.
     *
     * @param c
     *      character of type to promote to.
     * @return true if c represents a legal promotion type; false otherwise.
     *
     */
    private boolean isPromotableTo(char c) {
        return switch (c) { case 'Q', 'B', 'R', 'N' -> true; default -> false; };
    }
}