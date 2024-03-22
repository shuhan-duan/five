package com.five.utils;

public class FiveGameUtil {

    public static int isGameOver(Integer[][] chessBoard) {
        if (chessBoard == null) {
            return 0;
        }
        boolean isDeadHeat = true;

        for (int x = 0; x < chessBoard.length; x++) {
            for (int y = 0; y < chessBoard[x].length; y++) {
                Integer player = chessBoard[x][y];
                if (player == null || player == 0) {
                    isDeadHeat = false;
                }
                if (player != 0 && (checkDirection(chessBoard, x, y, 1, 0, player) ||
                        checkDirection(chessBoard, x, y, 0, 1, player) ||
                        checkDirection(chessBoard, x, y, 1, 1, player) ||
                        checkDirection(chessBoard, x, y, 1, -1, player))) {
                    return player;  // 返回获胜方
                }
            }
        }
        if (isDeadHeat) {
            return 3;   // 平局
        }
        return 0;  // 游戏尚未结束
    }


    private static boolean checkDirection(Integer[][] chessBoard, int x, int y, int deltaX, int deltaY, Integer player) {
        int count = 1;

        for (int i = 1; i < 5; i++) {
            int newX = x + i * deltaX;
            int newY = y + i * deltaY;
            if (!isValidPoint(chessBoard, newX, newY) || chessBoard[newX][newY] != player) {
                break;
            }
            count++;
        }

        for (int i = 1; i < 5; i++) {
            int newX = x - i * deltaX;
            int newY = y - i * deltaY;
            if (!isValidPoint(chessBoard, newX, newY) || chessBoard[newX][newY] != player) {
                break;
            }
            count++;
        }

        return count >= 5;
    }


    private static boolean isValidPoint(Integer[][] chessBoard, int x, int y) {
        return x >= 0 && x < chessBoard.length && y >= 0 && y < chessBoard[0].length;
    }

}


