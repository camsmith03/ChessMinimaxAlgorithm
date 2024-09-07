package com.github.camsmith03;
import java.util.Arrays;

/**
 * <p>
 *     Main recursive algorithm that builds a "psuedo-tree" from function calls.
 *     Hard set to a max ply of 7 as some inefficiencies in the static
 *     evaluation still need to be worked out to support deeper searches.
 * </p><p>
 *     Designed with an iterative deepening system with a starting ply of 1,
 *     (incrementing up one on each call to minimax(). Utilizes an Alpha-Beta
 *     pruning technique to drastically reduce search complexity, supporting a
 *     larger max ply or deeper search.
 * </p>
 *
 * @author Cameron Smith
 * @version 09.04.2024
 */
public class Minimax {
    private final Evaluator evaluator;
    private static final int STARTING_DEPTH = 1;
    private static final int MAX_PLY = 7;
    private static final int LOADING_BAR_PLY = 7;
    private static final int MID_GAME_DEPTH = 6;
    private int ply = STARTING_DEPTH;

    /**
     * Constructor for Minimax that takes in a BoardController (at the initial state).
     * Maximizer is set to boardController.getTurn(), so on object creation, it is
     * essential that the current move is intended to be the player.
     *
     * @param boardController
     *      Contains the initial boardController that will be utilized throughout
     *      execution.
     */
    public Minimax(BoardController boardController) {
        evaluator = new Evaluator(boardController.getBitboard(), boardController.getTurn(), ply);
    }


    /**
     * This is the algorithm invoked to start the search. It generates every
     * move at the given instance, then will recursively apply each move and
     * restore the initial state until a branch can possibly be pruned off.
     *
     * @param boardController
     *      Current configuration of the boardController on the given call.
     * @return Move
     *      Predicted, best move based on the alpha beta values calculated from
     *      the static evaluation.
     */
    public Move minimax(BoardController boardController) {
        if (ply == MID_GAME_DEPTH)
            evaluator.stopCentralBonus(); // prevents over-rewarding for the central squares once the game has reached
                                          // the 20+ move mark.

        // Need to apply the first level to the boardController, so we know which move has
        // the best subtree evaluation.
        Move move; Move bestBranch = null;
        int bestBranchEval = Integer.MIN_VALUE;
        MoveList moves = boardController.getLegalMoves();

        if (moves.isEmpty())
            throw new IllegalStateException("Moves list is empty");


        char[] loadingBar = new char[moves.size()];
        Arrays.fill(loadingBar, ' ');
        int movesDone = 0;


        if (ply == 1) {
            Bitboard bitboard = boardController.getBitboard();
            while (!moves.isEmpty()) {
                move = moves.pop();
                bitboard.virtualMovePiece(move);
                int eval = evaluator.evaluate(move);
                if (eval > bestBranchEval) {
                    bestBranchEval = eval;
                    bestBranch = move;
                }
                bitboard.wipeVirtualization();
            }
        }
        else {
            if (ply >= LOADING_BAR_PLY) { System.out.println(getLoadingBar(loadingBar, movesDone)); } // empty bar

            Piece.Color turnToMove = boardController.getTurn();
            while (!moves.isEmpty()) {

                move = moves.pop();
                try {
                    SaveState saveLoc = boardController.saveGameState();
                    boardController.applyMinimaxMove(move);
                    int subtreeVal = alphaBeta(boardController, 2, Integer.MIN_VALUE, Integer.MAX_VALUE);

                    if (subtreeVal > bestBranchEval) {
                        bestBranchEval = subtreeVal;
                        bestBranch = move;
                    }
                    boardController.restoreGameState(saveLoc, turnToMove);
                }
                catch (IllegalArgumentException e) {
                    // King attempted to put itself in check, ignore this branch
                }
                if (ply >= LOADING_BAR_PLY) { // only use the loading bar when wait will make the visual useful
                    System.out.println(getLoadingBar(loadingBar, ++movesDone));
                }
            }
        }

        if (bestBranch == null) { throw new IllegalStateException("bestBranch never initialized"); }

        if (ply <= MAX_PLY)
            ply += 1; // Iterative Deepening

        return bestBranch;
    }

    /**
     * Primary recursive algorithm for the Minimax class. This will search each
     * subtree until currDepth == ply, where it will then start performing
     * static evaluation, and returning up the tree.
     * Note: must be invoked with alpha < beta for proper usage.
     *
     *
     * @param boardController
     *      BoardController corresponding to the active instance when invoked.
     * @param currDepth
     *      Marker that will indicate when static eval occurs if it reaches the
     *      ply.
     * @param alpha
     *      Alpha value for pruning (start at Integer.MIN_VALUE)
     * @param beta
     *      Beta value for pruning (start at Integer.MAX_VALUE)
     * @return int
     *      Evaluation for the subtree.
     */
    private int alphaBeta(BoardController boardController, int currDepth, int alpha, int beta) {
        if (alpha >= beta) throw new IllegalStateException("alphaBeta shouldn't be invoked with alpha >= beta");

        MoveList moves = boardController.getLegalMoves();
        Move nextMove;

        if (currDepth < ply) {

            if (currDepth % 2 == 1) {
                // Currently on the MAX layer. Can change the alpha value.
                int nextAlpha;
                Piece.Color turnToMove = boardController.getTurn();
                SaveState saveLoc = boardController.saveGameState();

                while (!moves.isEmpty()) {

                    nextMove = moves.pop();
                    try {
                        boardController.applyMinimaxMove(nextMove); // throws exception on illegal move
                        nextAlpha = alphaBeta(boardController, currDepth + 1, alpha, beta);

                        boardController.restoreGameState(saveLoc, turnToMove);
                        if (nextAlpha != Integer.MAX_VALUE)
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
                Piece.Color turnToMove = boardController.getTurn();
                SaveState saveLoc = boardController.saveGameState();

                while (!moves.isEmpty()) {
                    nextMove = moves.pop();
                    try {
                        boardController.applyMinimaxMove(nextMove);
                        nextBeta = alphaBeta(boardController, currDepth + 1, alpha, beta);

                        boardController.restoreGameState(saveLoc, turnToMove);

                        if (nextBeta != Integer.MIN_VALUE)
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

        return evaluateMoves(boardController.getBitboard(), moves, (currDepth % 2 == 1), alpha, beta); // once the ply is reached, return the leaf eval.
    }

    /**
     * This method will be invoked whenever the depth hits the max ply. It will
     * generate all the current moves, evaluate the boardController at each move state,
     * then return the value corresponding to whether it is a max or min layer.
     *
     * @param bBoard
     *      Bitboard object representing the current state. This will be used
     *      for the move virtualization, which is a highly efficient way to
     *      temporarily apply moves without any hard copies.
     * @param moves
     *      MoveList containing all the moves at the current state
     * @param maxLayer
     *      Flag used to indicate if the leaves to be evaluated will be
     *      considered for the min or max layer.
     * @param alpha
     *      Current alpha value of the sub-branch. If maxLayer, we may be able
     *      to prune the branch.
     * @param beta
     *      Current beta value of the sub-branch. If not maxLayer, we may be
     *      able to prune the branch.
     * @return int
     *      maximum evaluation (if maxLayer), minimum evaluation (if not
     *      maxLayer)
     */
    private int evaluateMoves(Bitboard bBoard, MoveList moves, boolean maxLayer, int alpha, int beta) {
        int evaluation;

        if (maxLayer) {
            // On a MAX layer. Return up the new alpha value.

            while (!moves.isEmpty()) {
                Move move = moves.pop();
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

    /**
     * Basic loading bar used to indicate progress when the max ply is hit.
     * While not necessary, it provides a nice way to indicate progression and
     * add one additional depth layer without making it feel significantly
     * slower.
     *
     * @param loadingBar
     *      char array state (default is all space chars).
     * @param movesDone
     *      number of moves computed at the 2nd level.
     * @return String
     *      loading bar representation at the current stage.
     */
    private String getLoadingBar(char[] loadingBar, int movesDone) {
        StringBuilder out = new StringBuilder("Loading... [");

        if (movesDone > 0)
            loadingBar[movesDone - 1] = '=';

        out.append(loadingBar);
        out.append(']');

        return out.toString() ;
    }
}
