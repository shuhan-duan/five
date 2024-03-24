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

    private Integer game_total_counts;

    private Integer game_success_counts;

    private Integer game_fail_counts;

    private Integer game_dead_heat_counts;

    private Integer deleted;
}
