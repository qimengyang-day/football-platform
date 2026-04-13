package com.football.vo;

import lombok.Data;

@Data
public class FanPlayerListVO {
    private Long id; // player_user_id
    private String name; // realName
    private String clubName;
    private String position;
    private Integer age;
    private String nationality;
    private Double marketValue;
}

