package com.github.camsmith03;
import java.util.Scanner;

/**
 * Class housing the main I/O for the game itself. This simply passes most
 * information to the GameEngine, mainly serving to find which color the player
 * intends to try.
 *
 * @author Cameron Smith
 * @version 08.26.2024
 */
public class GameUI {
    public static void main(String[] args) {
        if (args.length != 0) {
            System.out.println("Usage: java GameUI");
            System.exit(1);
        }
        GameEngine gameEngine;
        Scanner scan = new Scanner(System.in);

        System.out.print("Are you playing white [Y/n]: ");
        String response = scan.nextLine().toLowerCase();
        if (response.equals("y") || response.equals("yes") || response.isEmpty()) {
            gameEngine = new GameEngine(Piece.Color.WHITE);
            gameEngine.probeFirstWhiteMove(scan);
        }
        else if (response.equals("n") || response.equals("no")) {
            gameEngine = new GameEngine(Piece.Color.BLACK);
            gameEngine.probeNextMove(scan);
        }
        else {
            System.out.println("Invalid input");
            System.exit(1);
        }
    }
}
