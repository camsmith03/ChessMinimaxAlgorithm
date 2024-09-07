package com.github.camsmith03;

// TODO:  Instead of this, utilize an array SaveState[ply - 1]. Then, each index
//        can be invoked and modified, and the entire array can be created at
//        the start of the program. This would mitigate GC inefficiencies.
//        I.E.: circular stack.
public class SaveState {
    final long[][] boards;
    final long[] colorBoards = new long[2];
    final long epBoard;

    public SaveState(long[][] boards, long[] colorBoards, long epBoard) {
        this.boards = new long[2][18];

        System.arraycopy(boards[0], 0, this.boards[0], 0, 18);
        System.arraycopy(boards[1], 0, this.boards[1], 0, 18);

        System.arraycopy(colorBoards, 0, this.colorBoards, 0, 2);

        this.epBoard = epBoard;
    }
}
