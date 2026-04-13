package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("football_match")
public class FootballMatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String cover;
    private String location;
    private Date startTime;
    private String status;
    private Long viewCount;
    private Long createBy;
    private Long leagueId;
    private Integer homeScore;
    private Integer awayScore;

    // 对战双方（俱乐部）
    private Long homeTeamId;
    private Long awayTeamId;

    // 兼容前端字段名（不落库）
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Date matchTime;

    // 详情展示用（不落库）
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String homeTeamName;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String awayTeamName;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String leagueName;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String matchResult;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String winnerTeamName;
}