package com.football.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String role;
    private String avatar;
    private String username;
    private String nickname;
    private boolean isDefaultPassword;
    // 俱乐部 Logo（仅 CLUB 角色）
    private String clubLogo;

    // Lombok 对 boolean 且字段以 is 开头时生成的 setter 名称可能为 setDefaultPassword，
    // 为兼容现有调用代码，补充一个显式 setter。
    public void setIsDefaultPassword(boolean isDefaultPassword) {
        this.isDefaultPassword = isDefaultPassword;
    }
}