package com.five.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;

    private String username;

    private String password;

    private Integer gameTotalCounts;

    private Integer gamePersonCounts;

    private Integer gameAiCounts;

    private Integer gameSuccessCounts;

    private Integer gameFailCounts;

    private Integer gameDeadHeatCounts;

    private Integer deleted;
}
