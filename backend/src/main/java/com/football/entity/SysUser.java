package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String password;
    private String role;
    private String avatar;
    private Integer starLevel;
    private String phone;
    private Integer status;
    private Date createTime;
    private Date lastLoginTime;
    private Long mainTeamId; // 主队 ID

    // 兼容前端详情页：运行时动态计算的主队名称（不落库）
    @TableField(exist = false)
    private String mainTeamName;

    @TableField("favorite_club_id")
    private Long favoriteClubId;
}