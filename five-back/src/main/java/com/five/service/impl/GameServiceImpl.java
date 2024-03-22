package com.five.service.impl;

import com.five.mapper.GameHistoryMapper;
import com.five.mapper.UserMapper;
import com.five.pojo.dto.GameAIMessageDTO;
import com.five.pojo.entity.GameHistory;
import com.five.pojo.entity.User;
import com.five.pojo.vo.GameAIMessageVO;
import com.five.pojo.vo.UserSelectByIdVO;
import com.five.service.GameService;
import com.five.utils.AIUtil;
import com.five.utils.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;


@Service
public class GameServiceImpl implements GameService {
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GameHistoryMapper gameHistoryMapper;

    @Override
    @Transactional
    public Long addAiGame(Long userId) {
        GameHistory gameHistory = GameHistory.builder()
                .blackId(userId)
                .whiteId(0L)
                .beginTime(LocalDateTime.now()).build();

        gameHistoryMapper.insert(gameHistory);

        return gameHistory.getId();
    }

    
    @Override
    @Transactional
    public GameAIMessageVO transferAiMessage(GameAIMessageDTO gameAIMessageDTO) {
        // Record AI thinking time
        LocalDateTime startTime = LocalDateTime.now();
        // Then call AIUtil to calculate the next move of AI. The data has been assembled in this utility method (calculate time is added externally)
        GameAIMessageVO gameAIMessageVO = AIUtil.getAIChoice(gameAIMessageDTO.getPlayerMoves(), gameAIMessageDTO.getLevel(), gameAIMessageDTO.getBoardStates(), gameAIMessageDTO.getStepOrder());
        LocalDateTime endTime = LocalDateTime.now();

        // Calculate the duration between the two time points
        Duration duration = Duration.between(startTime, endTime);
        long seconds = duration.getSeconds();

        // Set thinking time
        gameAIMessageVO.setThinkTime(seconds);

        // Check the current game status, modify game_history and user table after the game ends
        if (gameAIMessageVO.getGameStatus() != 3) { // The current game has ended: 0 user wins, 1 AI wins, 2 draw, 3 game in progress
            // Update the game history status in the database. No need to add AI moves to the database because AI hasn't made a move
            GameHistory gameHistory = GameHistory.builder()
                    .endTime(LocalDateTime.now())
                    .gameResult(gameAIMessageVO.getGameStatus())
                    .build();

            gameHistoryMapper.update(gameHistory);

            // Update the user table, add points to the user after the game ends
            Long userId = BaseContext.getCurrentId();
            UserSelectByIdVO user = userMapper.selectUserById(userId);
            // Get the user's game counts
            int gameTotalCounts = user.getGameTotalCounts();
            int gameAiCounts = user.getGameAiCounts();
            int gameSuccessCounts = user.getGameSuccessCounts();
            int gameFailCounts = user.getGameFailCounts();
            int gameDeadHeatCounts = user.getGameDeadHeatCounts();

            // Update game counts
            if (gameAIMessageVO.getGameStatus() == 0) { // User wins
                gameSuccessCounts += 1;
            } else if (gameAIMessageVO.getGameStatus() == 1) {  // User loses
                gameFailCounts += 1;
            } else if (gameAIMessageVO.getGameStatus() == 2) {  // Draw
                gameDeadHeatCounts += 1;
            }
            gameTotalCounts += 1;
            gameAiCounts += 1;

            // Assemble data
            User user1 = User.builder().
                    id(userId)
                    .gameTotalCounts(gameTotalCounts)
                    .gameAiCounts(gameAiCounts)
                    .gameSuccessCounts(gameSuccessCounts)
                    .gameFailCounts(gameFailCounts)
                    .gameDeadHeatCounts(gameDeadHeatCounts)
                    .build();
            userMapper.update(user1);   // Update
        }


        return gameAIMessageVO;
    }
}
