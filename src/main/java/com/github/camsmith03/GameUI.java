package com.github.camsmith03;
import java.util.Scanner;

public class GameUI {


    public static void main(String[] args) {
        if (args.length != 0) {
            System.out.println("Usage: java GameUI");
            System.exit(0);
        }
        GameEngine gameEngine;
        Scanner scan = new Scanner(System.in);


        System.out.print("Are you playing white [Y/n]: ");
        String response = scan.nextLine().toLowerCase();
        if (response.equals("y") || response.equals("yes") || response.isEmpty()) {
//            System.out.println("You picked white");
            gameEngine = new GameEngine(Piece.Color.WHITE);
            gameEngine.probeFirstWhiteMove(scan);
        }
        else if (response.equals("n") || response.equals("no")) {
//            System.out.println("You picked black");
            gameEngine = new GameEngine(Piece.Color.BLACK);
            gameEngine.probeNextMove(scan);
        }
        else {
            System.out.println("Invalid input");
            System.exit(0);
        }

    }



//            long start = System.nanoTime();
//            MoveList moveList= gameEngine.board.getLegalMoves();
//            long finish = System.nanoTime();
//            System.out.println("Time to generate all moves (ms): " + ((finish - start) / 1000000));
}
