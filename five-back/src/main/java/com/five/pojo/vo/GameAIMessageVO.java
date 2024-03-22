package com.five.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameAIMessageVO {

    private AICoordinate AIPieces;

    private Integer gameStatus;

    private Long thinkTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AICoordinate {
        private Byte x;

        private Byte y;
    }
}
