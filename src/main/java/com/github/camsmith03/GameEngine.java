package com.github.camsmith03;
import java.util.Scanner;

public class GameEngine {
    public Board board;
    private InputLexer lexer;
    private Minimax minimax;
    private int tempTurn;

    public GameEngine(Piece.Color userColor) {
        board = new Board();
        lexer = new InputLexer(board);
        minimax = new Minimax(userColor);
        tempTurn = 0; // TODO Delete this
    }

    /**
     * Probes the input scanner for the opponent's next move that was made. This will utilize the interpreter that will
     * take in a string parameter representing a move in chess notation, confirm the notation is legal, then use a
     * helper method to check and see if the current board allows for that given move.
     *
     * @param scan
     */
    public void probeNextMove(Scanner scan) {
        System.out.print("Please enter the opponent's move: ");
        String inputMove = scan.nextLine();
        try {
            String strippedInput = inputMove.replaceAll(" ", "").replaceAll("O","0");
            Move move = lexer.interpret(strippedInput);
            System.out.println("Successfully interpreted: " + inputMove);
            System.out.println("From Position: " + hexString(move.getFromMask()));
            System.out.println("To   Position: " + hexString(move.getToMask()));
            board.move(move);
            System.out.println("=================== GAME  BOARD ====================");
            board.getBitboard().printGameBoard();
            System.out.println("=================== WHITE BOARD ====================");
            board.getBitboard().printColorBoard(Piece.Color.WHITE);
            System.out.println("=================== BLACK BOARD ====================");
            board.getBitboard().printColorBoard(Piece.Color.BLACK);

            if (tempTurn < 100) { // TODO REMOVE THIS TESTING
                probeNextMove(scan);
                tempTurn += 1;
            }
        }
        catch (IllegalArgumentException e) {
            // Notation wasn't valid
            System.out.println(e.getMessage());
            probeNextMove(scan);
        }
        catch (IllegalStateException e) {
            // Notation was valid, but move was not legal
            System.out.println(e.getMessage());
            probeNextMove(scan);
        }
    }

    public static String hexString(long pos) {
        return String.format("0x%016x", pos);
    }
}
