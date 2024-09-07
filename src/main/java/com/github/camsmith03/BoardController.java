package com.github.camsmith03;

/**
 * Primary interface between the GameEngine and the bitboards. Some classes
 * require direct interfacing to the bitboard storage, so the option to obtain
 * and modify it is given.
 *
 * @author Cameron Smith
 * @version 08.26.2024
 */
public class BoardController {
    private Bitboard board;
    private Piece.Color turnToMove;
    private MoveGenerator moveGenerator;

    /**
     * Constructor for the BoardController that creates the bitboard, move
     * generator, and sets the turn flag to white.
     */
    public BoardController() {
        board = new Bitboard();
        moveGenerator = new MoveGenerator();
        turnToMove = Piece.Color.WHITE;
    }

    /**
     * Getter for Bitboard for some classes that require direct usage to the
     * underlying data structures.
     *
     * @return board
     */
    protected Bitboard getBitboard() {
        return board;
    }

    /**
     * Wipes the board, having a similar effect to re-invoking the constructor.
     */
    public void cleanBoard() {
        board = new Bitboard();
        moveGenerator = new MoveGenerator();
        turnToMove = Piece.Color.WHITE;
    }

    /**
     * Appends a move to the board, assuming the parameter given is valid for
     * the current configuration.
     *
     * @param move
     *      that will be applied to the bitboards.
     *
     * @throws IllegalArgumentException
     *      Thrown if the move is illegal for the current state of the board.
     */
    public void move(Move move) throws IllegalArgumentException {
        board.movePiece(move);
        changeTurn();
    }

    /**
     * Assuming the board is currently in a virtual state, the virtualization
     * will be written to the bitboards.
     */
    protected void applyVirtualMove() {
        board.applyVirtualMovePermanently();
        changeTurn();
    }

    /**
     * Returns a new SaveState object that contains a hard copy of all the long
     * arrays that store the bits of the bitboard configuration. NOTE: make sure
     * that the turnToMove is saved since it will become relevant when restoring
     * from a previous state.
     *
     * @return SaveState
     */
    public SaveState saveGameState() {
        return board.saveCurrentState();
    }

    /**
     * Returns the bitboard to an old state referenced by the saveState
     * parameter. This is a one-way operation and cannot be undone, so any data
     * that is relevant in the current state must be extracted before invoking
     * this method.
     *
     * @param saveState
     *      previous state the board was in.
     * @param turnToMove
     *      whose turn it was since the save state.
     */
    public void restoreGameState(SaveState saveState, Piece.Color turnToMove) {
        this.turnToMove = turnToMove;
        board.restoreSaveState(saveState);
    }

    /**
     * Applies move for the minimax algorithm. This will handle wiping the
     * virtual state if an illegal move is made. It will also apply the virtual
     * move permanently to ensure a lack of confusion.
     *
     * @param tempMove
     *      move to apply to the tempBoards (then permanent if successful)
     * @throws RuntimeException
     *      when the game has ended since the move captured one of the kings.
     */
    protected void applyMinimaxMove(Move tempMove) throws RuntimeException {
        try {
            board.virtualMovePiece(tempMove);
            if (board.kingMissing()) {
                throw new RuntimeException(); // game is over (one of the kings is no longer there)
            }
        } catch (IllegalArgumentException e) {
            board.wipeVirtualization();
            throw e;
        }
        board.applyVirtualMovePermanently();

        changeTurn();
    }

    /**
     * Changes the turn manually. Useful for input testing.
     */
    protected void changeTurn() {
        if (turnToMove == Piece.Color.WHITE) {
            turnToMove = Piece.Color.BLACK;
        }
        else {
            turnToMove = Piece.Color.WHITE;
        }
    }

    /**
     * Getter for the current turn for the board state.
     *
     * @return turnToMove.
     */
    public Piece.Color getTurn() {
        return turnToMove;
    }

    /**
     * Invokes the move generator on the current board and returns the MoveList
     * that is associated with all legal moves in the current configuration.
     *
     * @return MoveList
     */
    public MoveList getLegalMoves() {
        return moveGenerator.generateMoves(board, turnToMove);
    }

    /**
     * Getter for a piece at any given position.
     *
     * @param position
     *      masking bit for any square on the chess board
     * @return Piece if it exists; null if not.
     */
    public Piece getPiece(long position) {
        return board.getPiece(position);
    }
}