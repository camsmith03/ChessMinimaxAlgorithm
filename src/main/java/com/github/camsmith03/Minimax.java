package com.github.camsmith03;

public class Minimax {
    private final Evaluator evaluator;
    private final OutputTranslationUnit otu;
    public static final int STARTING_DEPTH = 1;
    public static final int MAX_PLY = 7;
    public static final int MID_GAME_DEPTH = 6;
    private int ply = STARTING_DEPTH;


    public Minimax(Board board) {
        evaluator = new Evaluator(board.getBitboard(), board.getTurn(), ply);
        otu = new OutputTranslationUnit();
    }

    // input board.getTurn() will be assumed to be the maximizer
    public Move minimax(Board board) {
        if (ply == MID_GAME_DEPTH)
            evaluator.stopCentralBonus(); // prevents over-rewarding for the central squares once the game has reached
                                          // the 20+ move mark.

        // Need to apply the first level to the board, so we know which move has the best subtree evaluation
        Move move; Move bestBranch = null;
        int bestBranchEval = Integer.MIN_VALUE;
        MoveList moves = board.getLegalMoves();
        if (moves.isEmpty())
            throw new IllegalStateException("Moves list is empty");

        int movesLeft = moves.size();
        int movesDone = 0;

        if (ply == 1) {
            Bitboard bitboard = board.getBitboard();
            while (!moves.isEmpty()) {
                move = moves.pop();
                bitboard.virtualMovePiece(move);
                int eval = evaluator.evaluate(move);
                if (eval > bestBranchEval) {
                    bestBranchEval = eval;
                    bestBranch = move;
//                    System.out.println("Best branch: " + otu.translate(bestBranch));
                }
                bitboard.wipeVirtualization();
//                System.out.println(getLoadingBar(--movesLeft, ++movesDone));
            }

        }
        else {
            if (ply == MAX_PLY) System.out.println(getLoadingBar(movesLeft, movesDone));
            Piece.Color turnToMove = board.getTurn();
            while (!moves.isEmpty()) {

                move = moves.pop();
                try {
                    SaveState saveLoc = board.saveGameState();
                    board.applyMinimaxMove(move);
                    int subtreeVal = alphaBeta(board, 2, Integer.MIN_VALUE, Integer.MAX_VALUE);

                    if (subtreeVal > bestBranchEval) {
                        bestBranchEval = subtreeVal;
                        bestBranch = move;
//                        System.out.println("Best branch: " + otu.translate(bestBranch));
//                        System.out.println("Evaluation: " + bestBranchEval + "\n");
                    }
                    board.restoreGameState(saveLoc, turnToMove);
                }
                catch (IllegalArgumentException e) {
                    // King attempted to put itself in check, ignore this branch
                }
                if (ply == MAX_PLY) System.out.println(getLoadingBar(--movesLeft, ++movesDone));
            }
        }

        if (bestBranch == null) throw new IllegalStateException("bestBranch never initialized");
        if (ply != MAX_PLY) ply += 1; // Iterative Deepening
        return bestBranch;
    }

    // Recursive searching method
    private int alphaBeta(Board board, int currDepth, int alpha, int beta) {
        if (alpha >= beta) throw new IllegalStateException("alphaBeta shouldn't be invoked with alpha >= beta");

        MoveList moves = board.getLegalMoves();
        Move nextMove;

        if (currDepth < ply) {
            // Need to prune out branches that qualify

            if (currDepth % 2 == 1) {
                // Currently on the MAX layer. Can change the alpha value.
                int nextAlpha;
                Piece.Color turnToMove = board.getTurn();
                SaveState saveLoc = board.saveGameState();
//                System.out.println("=== MAX LAYER SEARCH ===");

                while (!moves.isEmpty()) {
                    nextMove = moves.pop();
//                    System.out.println(nextMove.toString());
                    try {
                        board.applyMinimaxMove(nextMove); // throws exception on illegal move
                        nextAlpha = alphaBeta(board, currDepth + 1, alpha, beta);

                        board.restoreGameState(saveLoc, turnToMove);

                        alpha = Math.max(alpha, nextAlpha);

                        if (alpha >= beta)
                            return alpha; // prune the remaining moves
                    }
                    catch (IllegalArgumentException e) { /* Ignore paths for illegal moves */ }
                    catch (RuntimeException re) {
                        // Enemy king is captured after this move, heavily incentivize this path without recursive call.
                        return Integer.MAX_VALUE;
                    }
                }
                return alpha;
            }
            else {
                // Currently on the MIN layer. Can change the beta value.
                int nextBeta;
                Piece.Color turnToMove = board.getTurn();
                SaveState saveLoc = board.saveGameState();
//                System.out.println("=== MIN LAYER SEARCH ===");

                while (!moves.isEmpty()) {
                    nextMove = moves.pop();
//                    System.out.println(nextMove.toString());

                    try {
                        board.applyMinimaxMove(nextMove);
                        nextBeta = alphaBeta(board, currDepth + 1, alpha, beta);

                        board.restoreGameState(saveLoc, turnToMove);

                        beta = Math.min(beta, nextBeta);

                        if (alpha >= beta)
                            return beta; // prune the remaining moves
                    } catch (IllegalArgumentException e) { /* Ignore paths for illegal moves */ }
                    catch (RuntimeException re) {
                        // Enemy king is captured after this move, heavily incentivize this path without recursive call.
                        return Integer.MIN_VALUE;
                    }
                }
                return beta;
            }
        }
        // currDepth == maxDepth
        // We have hit the terminal depth, can now statically evaluate all possible moves.
        // We can still prune.


        int evaluation;
        Bitboard bBoard = board.getBitboard();
//        System.out.println("=== LEAF EVAL ===");

        if (currDepth % 2 == 1) {
            // On a MAX layer. Return up the new alpha value.

            while (!moves.isEmpty()) {
                Move move = moves.pop();
//                System.out.println(move.toString());
                try {
                    bBoard.virtualMovePiece(move);
                    evaluation = evaluator.evaluate(move);
                    alpha = Math.max(alpha, evaluation);

                    if (alpha >= beta)
                        return alpha;
                    bBoard.wipeVirtualization();

                } catch (IllegalArgumentException e) {  /*  TODO: De-incentivize paths that lead to illegal options */}
            }
            return alpha;
        }

        // On a MIN layer. Return up the new beta value.
        while (!moves.isEmpty()) {
            Move move = moves.pop();
//            if (move.toString().equals("Move [from=0x1000000000000000, to=0x0008000000000000]")) {
//                System.out.println("here");
//            }
//            System.out.println(move.toString());
            try {
                bBoard.virtualMovePiece(move);
                evaluation = evaluator.evaluate(move);
                beta = Math.min(beta, evaluation);

                if (alpha >= beta)
                    return beta;
            } catch (IllegalArgumentException e) {  /*  TODO: De-incentivize paths that lead to illegal options */}
            bBoard.wipeVirtualization();



        }

        return beta;

    }

    private int evaluateMoves(Board board, int alpha, int beta) {
        int evaluation;
        MoveList moves = board.getLegalMoves();
        Bitboard bBoard = board.getBitboard();
        Move nextMove;

        if (ply % 2 == 1) {
            // On a MAX layer. Return up the new alpha value.

            while (!moves.isEmpty()) {
                nextMove = moves.pop();
                if (bBoard.isMoveLegal(nextMove)) {
                    evaluation = evaluator.evaluate(moves.pop()); // send evaluator the move with board in virtual state
                    alpha = Math.max(alpha, evaluation);
                    if (alpha >= beta)
                        return alpha;
                }
            }
            return alpha;
        }

        // On a MIN layer. Return up the new beta value.
        while (!moves.isEmpty()) {
            evaluation = evaluator.evaluate(moves.pop());

            beta = Math.min(beta, evaluation);

            if (alpha >= beta)
                return beta;
        }

        return beta;
    }

    private String getLoadingBar(int movesLeft, int movesDone) {
        StringBuilder out = new StringBuilder("Loading... [");
        int total = movesLeft + movesDone;
        while (total > 0) {
            if (movesDone > 0) {
                out.append("=");
                movesDone -= 1;
            }
            else {
                out.append(" ");
            }
            total -= 1;
        }
        out.append("]");

        return out.toString();
    }
}
