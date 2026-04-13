package com.football.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private String avatar;
    private String phone;
    private Long mainTeamId;
    private Long favoriteClubId;
    // 球员特有信息
    private String realName;
    private Integer height;
    private Integer weight;
    private String position;
    // 俱乐部特有信息
    private String clubName;
    private String clubLogo;
}