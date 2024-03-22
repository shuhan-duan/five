package com.five.pojo.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineFiveMessage {
    private Integer type;
    private String role;
    private Integer stepOrder;
    private Long gameId;
    private String message;
}
