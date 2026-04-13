package com.football.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String nickname;
    private String password;
    private String role;
    private String phone;
    // 俱乐部注册必填：俱乐部名称（全站唯一）
    private String clubName;
}