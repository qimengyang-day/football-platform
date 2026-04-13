package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("match_rating")
public class MatchRating {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long matchId;
    private Long fanUserId;
    private Integer stars;
    private String comment;
    private Date createTime;
}

