package com.five;

import com.five.utils.FiveGameUtil;
import com.five.utils.GameResult;
import org.junit.jupiter.api.Test;

public class BoardTest {
    @Test
    public void test() {
        Integer[][] board = new Integer[15][15];
        for (int i = 0; i < board.length; i++) {
            for (int i1 = 0; i1 < board.length; i1++) {
                board[i][i1] = 0;
            }
        }
        for (int i = 1; i < 6; i++) {
            board[i][0] = 1;
            board[i][1] = 2;
        }
        board[1][1] = 0;
        GameResult game = FiveGameUtil.isGameOver(board);
        System.out.println(game.name());

    }
}
