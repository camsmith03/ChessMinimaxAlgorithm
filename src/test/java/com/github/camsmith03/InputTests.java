package com.github.camsmith03;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InputTests {
    BoardController boardController;
    InputLexer lex;
    char[] ranks = {'1', '2', '3', '4', '5', '6', '7', '8'};
    char[] files = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};

    @BeforeEach
    void setUp() {
        boardController = new BoardController();
        lex = new InputLexer(boardController);
    }

    @Test
    void testIllegalLexerInput() {

    }

    @Test
    void testIllegalPawnMoves() {

    }

    @Test
    void testLegalKnightMoves() {
        String[][] inclusionByColor = new String[2][];
        inclusionByColor[0] = new String[]{"Na3", "Nc3", "Nf3", "Nh3"};
        inclusionByColor[1] = new String[]{"Na6", "Nc6", "Nf6", "Nh6"};
        for (int j = 0; j < inclusionByColor[0].length; j++) {
            IllegalStateException ise = null;
            try {
                lex.interpret(inclusionByColor[0][j]);
            }
            catch (IllegalStateException e) {
                ise = e;
            }
            assertNull(ise);
            setUp();
        }

        for (int i = 0; i < inclusionByColor[1].length; i++) {
            boardController.changeTurn();
            IllegalStateException ise = null;
            try {
                lex.interpret(inclusionByColor[1][i]);
            }
            catch (IllegalStateException e) {
                ise = e;
            }
            assertNull(ise);
            setUp();
        }


    }

    @Test
    void testIllegalKnightMoves() {
        String[][] exclusionByColor = new String[2][];
        exclusionByColor[0] = new String[]{"Na3", "Nc3", "Nf3", "Na3"};
        exclusionByColor[1] = new String[]{"Na6", "Nc6", "Nf6", "Na6"};

        // Test both white and black side legality
        for (int i = 0; i < 2; i++) {
            // Test without capture
            moveHelper("B", exclusionByColor[i]);
            // Test with capture
            moveHelper("Bx", exclusionByColor[i]);

            // single ambiguity
            // no capture
            helperDup1("B",  true, false, exclusionByColor[i]); // by file
            helperDup1("B", false, false, exclusionByColor[i]); // by rank
            // capture
            helperDup1("B", true, true, exclusionByColor[i]); // by file
            helperDup1("B", false, true, exclusionByColor[i]); // by rank

            // double ambiguity
            helperDup2("B", false, exclusionByColor[i]); // no capture
            helperDup2("B", true, exclusionByColor[i]); // capture
            boardController.changeTurn();
        }
    }

    @Test
    void testIllegalBishopMoves() {
        // Test both white and black side legality
        for (int i = 0; i < 2; i++) {
            // Test without capture
            moveHelper("B", null);
            // Test with capture
            moveHelper("Bx", null);

            // single ambiguity
            // no capture
            helperDup1("B",  true, false, null); // by file
            helperDup1("B", false, false, null); // by rank
            // capture
            helperDup1("B", true, true, null); // by file
            helperDup1("B", false, true, null); // by rank

            // double ambiguity
            helperDup2("B", false, null); // no capture
            helperDup2("B", true, null); // capture
            boardController.changeTurn();
        }
    }

    @Test
    void testIllegalQueenMoves() {
        // Test both white and black side legality
        for (int i = 0; i < 2; i++) {
            // Test without capture
            moveHelper("Q", null);
            // Test with capture
            moveHelper("Qx", null);

            // single ambiguity
            // no capture
            helperDup1("Q",  true, false, null); // by file
            helperDup1("Q", false, false, null); // by rank
            // capture
            helperDup1("Q",  true, true, null); // by file
            helperDup1("Q", false, true, null); // by rank

            // double ambiguity
            helperDup2("Q", false, null); // no capture
            helperDup2("Q", true, null); // capture
            boardController.changeTurn();
        }
    }

    @Test
    void testIllegalRookMoves() {
        // Test both white and black side legality
        for (int i = 0; i < 2; i++) {
            // Test without capture
            moveHelper("R", null);
            // Test with capture
            moveHelper("Rx", null);

            // single ambiguity
            // no capture
            helperDup1("R",  true, false, null); // by file
            helperDup1("R", false, false, null); // by rank
            // capture
            helperDup1("R",  true, true, null); // by file
            helperDup1("R", false, true, null); // by rank

            // double ambiguity
            helperDup2("R", false, null); // no capture
            helperDup2("R",  true, null); // capture
            boardController.changeTurn();
        }
    }

    @Test
    void testIllegalKingMoves() {

    }


    private void helperDup1(String piece, boolean byFile, boolean capture, String[] exclude) {
        char[] diff;
        if (byFile)
            diff = files;
        else
            diff = ranks;


        for (int i = 0; i < 8; i++) {
            if (capture) {
                moveHelper(piece + diff[i] + 'x', exclude);
            }
            else {
                moveHelper(piece + diff[i], exclude);
            }
        }

    }

    private void helperDup2(String piece, boolean capture, String[] exclude) {
        for (int i = 0; i <= 7; i++) {
            helperDup1(piece + files[i], false, capture, exclude);
        }
    }

    private void moveHelper(String piece, String[] exclude) {
        IllegalStateException ise = null;
        for(int i = 0; i <= 7; i++) {
            for (int j = 0; j <= 7; j++) {
                try {
//                    String test = "Qa1";
//                    if (test.equals(piece + files[i] + ranks[j])) {
//                        System.out.println("Broken");
//                    }
//                    System.out.println(piece + files[i] + ranks[j]);
                    if (exclude == null) {
                        lex.interpret(piece + files[i] + ranks[j]);
                    }
                    else if (!arrContains(exclude, piece + files[i] + ranks[j])) {
                        lex.interpret(piece + files[i] + ranks[j]);
                    }

                } catch (IllegalStateException e) {
                    ise = e;
                }
                assertNotNull(ise);
                ise = null;
            }
        }
    }

    private boolean arrContains(String[] arr, String check) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(check)) {
                return true;
            }
        }
        return false;
    }
}
