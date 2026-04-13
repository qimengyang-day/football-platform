package com.football.dto;

import lombok.Data;

@Data
public class FanCommentCreateDTO {
    private Long matchId;
    private String content;
}

