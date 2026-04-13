package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("fan_follow_club")
public class FanFollowClub {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fanUserId;

    private Long teamClubId;

    private Date createTime;
}

