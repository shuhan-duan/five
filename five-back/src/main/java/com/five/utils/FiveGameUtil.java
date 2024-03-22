package com.five.utils;
import static com.five.utils.GameResult.*;

public class FiveGameUtil {

    private static final int WIN_CONDITION = 5;

    public static GameResult isGameOver(Integer[][] chessBoard) {
        if (chessBoard == null) {
            return CONTINUE;
        }
        boolean isDeadHeat = true;

        for (int x = 0; x < chessBoard.length; x++) {
            for (int y = 0; y < chessBoard[x].length; y++) {
                Integer player = chessBoard[x][y];
                if (player == null || player == 0) {
                    isDeadHeat = false;
                    continue;
                }
                // Check if the current player has five in a row in any direction.
                if (player != 0 && (hasFiveInARow(chessBoard, x, y, 1, 0, player) ||
                        hasFiveInARow(chessBoard, x, y, 0, 1, player) ||
                        hasFiveInARow(chessBoard, x, y, 1, 1, player) ||
                        hasFiveInARow(chessBoard, x, y, 1, -1, player))) {
                    return getGameResultFromPlayer(player);
                }
            }
        }
        return isDeadHeat ? DRAW : CONTINUE;
    }

    private static boolean hasFiveInARow(Integer[][] chessBoard, int x, int y, int deltaX, int deltaY, Integer player) {
        int count = 1;

        count += countConsecutivePieces(chessBoard, x, y, deltaX, deltaY, player);
        count += countConsecutivePieces(chessBoard, x, y, -deltaX, -deltaY, player);

        return count >= WIN_CONDITION;
    }

    private static int countConsecutivePieces(Integer[][] chessBoard, int x, int y, int deltaX, int deltaY, Integer player) {
        int count = 0;
        for (int i = 1; i < WIN_CONDITION; i++) {
            int newX = x + i * deltaX;
            int newY = y + i * deltaY;
            if (!isValidPoint(chessBoard, newX, newY) || !player.equals(chessBoard[newX][newY])) {
                break;
            }
            count++;
        }
        return count;
    }

    private static boolean isValidPoint(Integer[][] chessBoard, int x, int y) {
        return x >= 0 && x < chessBoard.length && y >= 0 && y < chessBoard[0].length;
    }


}


