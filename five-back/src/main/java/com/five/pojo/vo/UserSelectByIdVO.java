package com.five.pojo.vo;

import lombok.Data;

@Data
public class UserSelectByIdVO {

    private long id;

    private String username;

    private int gameTotalCounts;

    private int gamePersonCounts;

    private int gameAiCounts;

    private int gameSuccessCounts;

    private int gameFailCounts;

    private int gameDeadHeatCounts;

}
