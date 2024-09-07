package com.github.camsmith03;
import java.util.Scanner;

/**
 * Primary interface between the user and the minimax algorithm.
 *
 * @author Cameron Smith
 * @version 08.26.2024
 */
public class GameEngine {
    private final BoardController boardController;
    private final InputLexer lexer;
    private final Minimax minimax;
    private final Piece.Color playerColor;
    private final OutputTranslationUnit otu;

    public GameEngine(Piece.Color playerColor) {
        boardController = new BoardController();
        lexer = new InputLexer(boardController);
        minimax = new Minimax(boardController);
        otu = new OutputTranslationUnit();
        this.playerColor = playerColor;
    }

    /**
     * Probes the input scanner for the opponent's next move that was made. This
     * will utilize the interpreter that will take in a string parameter
     * representing a move in chess notation, confirm the notation is legal,
     * then use a helper method to check and see if the current boardController
     * allows for that given move.
     *
     * @param scan
     *      Scanner instance for stdin.
     */
    public void probeNextMove(Scanner scan) {
        if (playerColor == Piece.Color.BLACK)
            System.out.print("Please enter the white's move: ");
        else
            System.out.print("Please enter the black's move: ");

        String inputMove = scan.nextLine();
        switch (inputMove) {
            case "print white masks" -> {
                System.out.println("=================== WHITE MASKS ====================");
                boardController.getBitboard().printKingMasks(Piece.Color.WHITE);
                probeNextMove(scan);
            }
            case "print black masks" -> {
                System.out.println("=================== BLACK MASKS ====================");
                boardController.getBitboard().printKingMasks(Piece.Color.BLACK);
                probeNextMove(scan);
            }
            case "print board" -> {
                System.out.println("=================== GAME  BOARD ====================");
                boardController.getBitboard().printGameBoard();
                probeNextMove(scan);
            }
            case "print board bits" -> {
                System.out.println("=================== BIT   BOARD ====================");
                boardController.getBitboard().printGameBoardBits();
                probeNextMove(scan);
            }
            case "print white board" -> {
                System.out.println("=================== WHITE BOARD ====================");
                boardController.getBitboard().printColorBoard(Piece.Color.WHITE);
                probeNextMove(scan);
            }
            case "print black board" -> {
                System.out.println("=================== BLACK BOARD ====================");
                boardController.getBitboard().printColorBoard(Piece.Color.BLACK);
                probeNextMove(scan);
            }
            case "print all boards" -> {
                System.out.println(boardController.getBitboard().toString());
                probeNextMove(scan);
            }
        }



        try {
            lexer.interpret(inputMove.replaceAll(" ", "").replaceAll("O","0"));
            System.out.println("Successfully interpreted: " + inputMove);
            boardController.applyVirtualMove();
            System.out.println("\nBest move to make: " + getBestMove() + "\n");
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

    /**
     * Separate method used to find the first white move to make, since it
     * requires no user IO to find the opposing move.
     *
     * @param scan
     *      Scanner instance of stdin.
     */
    public void probeFirstWhiteMove(Scanner scan) {
        System.out.println("\nBest move to make: " + getBestMove() + "\n");
        probeNextMove(scan);
    }

    /**
     * Calls minimax to find the best move to make, then applies said move to
     * the board. Finally, it will call the output translator to convert it to
     * square-based notation.
     *
     * @return otu output
     */
    public String getBestMove() {
        Move bestMove = minimax.minimax(boardController);
        boardController.move(bestMove);
        return otu.translate(bestMove);
    }

    /**
     * Publicly accessible static method that will convert a long to a 64 bit
     * padded hexadecimal value. This is useful for the purposes of displaying
     * the computed masking bits
     *
     * @param pos
     *      long to convert to hex string.
     * @return hex-like string
     */
    public static String hexString(long pos) {
        return String.format("0x%016x", pos);
    }
}
