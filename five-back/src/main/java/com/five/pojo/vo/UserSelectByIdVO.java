package com.five.pojo.vo;

import lombok.Data;

@Data
public class UserSelectByIdVO {

    private long id;

    private String username;

    private int game_total_counts;
    
    private int game_success_counts;

    private int game_fail_counts;

    private int game_dead_heat_counts;

}
