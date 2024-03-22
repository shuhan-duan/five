package com.five.utils;

import com.five.pojo.pojo.GameCoordinate;
import com.five.pojo.dto.GameAIMessageDTO;
import com.five.pojo.vo.GameAIMessageVO;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class AIUtil {

    public static GameAIMessageVO getAIChoice(GameAIMessageDTO.AICoordinate lastMove, Integer level, Byte[][] boardStates, Integer stepOrder) {
        log.info("AI starts calculating the next coordinate, current search depth is {}", level);
        // Pass the boardStates and level to the constructor
        ChessBoardEvaluator calChessBoard = new ChessBoardEvaluator(boardStates, level);
        GameCoordinate moves = new GameCoordinate(lastMove.getX(), lastMove.getY());

        // First, check if the opponent has won
        byte gameStatus = calChessBoard.isGameOver(moves);
        if (gameStatus == ChessBoardEvaluator.BLACK) {
            return GameAIMessageVO.builder().gameStatus(0).build();
        }
        // Check if it's a draw
        if (isDeadHeat(boardStates)) {
            // Draw
            return GameAIMessageVO.builder().gameStatus(2).build();
        }

        // If it's not a draw and the opponent hasn't won, continue playing
        GameAIMessageVO work = calChessBoard.work();
        return work;

    }

    private static boolean isDeadHeat(Byte[][] boardStates) {

        boolean flag = true;
        for (Byte[] boardState : boardStates) {
            for (Byte aByte : boardState) {
                if (aByte == 0) {
                    flag = false;
                    break;
                }
            }
        }
        return flag;
    }

}
