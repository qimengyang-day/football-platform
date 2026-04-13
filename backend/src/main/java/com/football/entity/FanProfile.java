package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("fan_profile")
public class FanProfile {
    @TableId(type = IdType.INPUT)
    private Long userId;

    // 对应 team_club.id
    private Long mainTeamId;

    // 星级 0-5
    private Integer starLevel;
}

