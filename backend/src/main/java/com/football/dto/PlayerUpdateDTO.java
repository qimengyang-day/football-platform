package com.football.dto;

import lombok.Data;

@Data
public class PlayerUpdateDTO {
    private Long id;
    private Long userId;
    private String realName;
    private String username;
    private Integer height;
    private Integer weight;
    private String position;
    private Long teamId;
    private Double marketValue;
    private Integer isFreeAgent;
}
