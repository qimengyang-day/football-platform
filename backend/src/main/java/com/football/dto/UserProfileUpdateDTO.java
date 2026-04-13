package com.football.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDTO {
    private String nickname;
    private String avatar;
    private Long mainTeamId;
    private Long favoriteClubId;
}

