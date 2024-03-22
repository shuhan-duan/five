package com.five.pojo.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameAIMessageDTO {

    private AICoordinate playerMoves;

    private Integer stepOrder;

    private Integer level;

    private Long gameId;

    private Byte[][] boardStates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AICoordinate {
        private Byte x;

        private Byte y;
    }

}
