package com.five.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameHistory {

    private Long id;

    private Long roomId;

    private Long blackId;

    private Long whiteId;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private Integer gameResult;

}
