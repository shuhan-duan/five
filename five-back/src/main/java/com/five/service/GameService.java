package com.five.service;

import com.five.pojo.dto.GameAIMessageDTO;
import com.five.pojo.vo.GameAIMessageVO;

public interface GameService {

    Long addAiGame(Long userId);

    GameAIMessageVO transferAiMessage(GameAIMessageDTO gameAIMessageDTO);
}
