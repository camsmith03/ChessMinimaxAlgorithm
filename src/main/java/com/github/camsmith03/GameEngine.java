package com.github.camsmith03;
import java.util.Scanner;

public class GameEngine {
    public final Board board;
    private final InputLexer lexer;
    private final Minimax minimax;
    private int tempTurn;
    private final Piece.Color playerColor;
    private final OutputTranslationUnit otu;

    public GameEngine(Piece.Color playerColor) {
        board = new Board();
        lexer = new InputLexer(board);
        minimax = new Minimax(board);
        tempTurn = 0; // TODO Delete this
        otu = new OutputTranslationUnit();
        this.playerColor = playerColor;
    }

    /**
     * Probes the input scanner for the opponent's next move that was made. This will utilize the interpreter that will
     * take in a string parameter representing a move in chess notation, confirm the notation is legal, then use a
     * helper method to check and see if the current board allows for that given move.
     *
     * @param scan
     */
    public void probeNextMove(Scanner scan) {
        if (playerColor == Piece.Color.BLACK) {
            System.out.print("Please enter the white's move: ");
        }
        else {
            System.out.print("Please enter the black's move: ");
        }
        String inputMove = scan.nextLine();
        if (inputMove.equals("print white masks")) {
            System.out.println("=================== WHITE MASKS ====================");
            board.getBitboard().printKingMasks(Piece.Color.WHITE);
            probeNextMove(scan);
        }
        else if (inputMove.equals("print black masks")) {
            System.out.println("=================== BLACK MASKS ====================");
            board.getBitboard().printKingMasks(Piece.Color.BLACK);
            probeNextMove(scan);
        }
        else if (inputMove.equals("print board")) {
            System.out.println("=================== GAME  BOARD ====================");
            board.getBitboard().printGameBoard();
            probeNextMove(scan);
        }
        else if (inputMove.equals("print board bits")) {
            System.out.println("=================== BIT   BOARD ====================");
            board.getBitboard().printGameBoardBits();
            probeNextMove(scan);
        }
        else if (inputMove.equals("print white board")) {
            System.out.println("=================== WHITE BOARD ====================");
            board.getBitboard().printColorBoard(Piece.Color.WHITE);
            probeNextMove(scan);
        }
        else if (inputMove.equals("print black board")) {
            System.out.println("=================== BLACK BOARD ====================");
            board.getBitboard().printColorBoard(Piece.Color.BLACK);
            probeNextMove(scan);
        }
        else if (inputMove.equals("print all boards")) {
            board.getBitboard().printAllGameBoards();
            probeNextMove(scan);
        }



        try {
            Move move = lexer.interpret(inputMove.replaceAll(" ", "").replaceAll("O","0"));
            System.out.println("Successfully interpreted: " + inputMove);
//            System.out.println("From Position: " + hexString(move.getFromMask()));
//            System.out.println("To   Position: " + hexString(move.getToMask()));
            board.applyVirtualMove();
            System.out.println("\nBest move to make: " + getBestMove() + "\n");

            if (tempTurn < 100) { // TODO REMOVE THIS
                tempTurn += 1;
                probeNextMove(scan);
            }
        }
        catch (IllegalArgumentException e) {
            // Notation wasn't valid
            e.printStackTrace();
            System.out.println(e.getMessage());
            probeNextMove(scan);
        }
        catch (IllegalStateException e) {
            // Notation was valid, but move was not legal
            e.printStackTrace();
            System.out.println(e.getMessage());
            probeNextMove(scan);
        }
    }

    public void probeFirstWhiteMove(Scanner scan) {
        System.out.println("\nBest move to make: " + getBestMove() + "\n");
        probeNextMove(scan);
    }

    public String getBestMove() {
        Move bestMove = minimax.minimax(board);
        board.move(bestMove);
        return otu.translate(bestMove);
    }

    public static String hexString(long pos) {
        return String.format("0x%016x", pos);
    }
}
