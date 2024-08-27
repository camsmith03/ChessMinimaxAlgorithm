package com.github.camsmith03;
import java.util.Arrays;

public class SaveState {
    final long[][] boards;
    final long[] colorBoards = new long[2];
    final long epBoard;

    public SaveState(long[][] boards, long[] colorBoards, long epBoard) {
        this.boards = new long[2][18];
//        for (int i = 0; i < 2; i++) {
//            this.boards[i] = new long[boards[i].length];
//            for (int j = 0; j < boards[i].length; j++) {
//                this.boards[i][j] = boards[i][j];
//            }
//        }
        System.arraycopy(boards[0], 0, this.boards[0], 0, 18);
        System.arraycopy(boards[1], 0, this.boards[1], 0, 18);

        System.arraycopy(colorBoards, 0, this.colorBoards, 0, 2);
//        this.colorBoards[0] = colorBoards[0];
//        this.colorBoards[1] = colorBoards[1];
        this.epBoard = epBoard;
    }
}
