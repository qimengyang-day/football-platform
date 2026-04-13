package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("match_registration")
public class MatchRegistration {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchId;

    private Long teamClubId;

    private Long userId; // 当前报名的俱乐部管理员user_id

    private String status;

    private Date createTime;
}

