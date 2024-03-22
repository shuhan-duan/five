package com.five.controller;

import com.five.pojo.pojo.Result;
import com.five.pojo.vo.GameAIMessageVO;
import com.five.pojo.dto.GameAIMessageDTO;
import com.five.service.GameService;
import com.five.utils.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/game")
@Slf4j

public class GameController {
    @Autowired
    private GameService gameService;

    @PostMapping("/ai/games")
    public Result<Long> addAiGame() {
        Long userId = BaseContext.getCurrentId();
        log.info("User {} adds a chess game", userId);
        Long gameId = gameService.addAiGame(userId);
        return Result.success(gameId);
    }

    @PostMapping("/ai/pieces")
    public Result<GameAIMessageVO> transferAiMessage(@RequestBody GameAIMessageDTO gameAIMessageDTO) {
        log.info("Data transmission for AI chess game, transmitted message: {}", gameAIMessageDTO);
        GameAIMessageVO gameAIMessageVO = gameService.transferAiMessage(gameAIMessageDTO);
        log.info("Return of AI chess move data transmission, returned message: {}", gameAIMessageVO);
        return Result.success(gameAIMessageVO);
    }
}
