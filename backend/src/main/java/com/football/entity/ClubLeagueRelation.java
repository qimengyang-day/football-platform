package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("club_league_relation")
public class ClubLeagueRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clubId;
    private Long leagueId;
    private Date joinTime;
}