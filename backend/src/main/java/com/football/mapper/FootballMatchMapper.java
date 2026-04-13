package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.football.entity.FootballMatch;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FootballMatchMapper extends BaseMapper<FootballMatch> {
    
    /**
     * 根据俱乐部 ID 查询比赛（使用 MyBatis-Plus 方式避免动态 SQL 问题）
     */
    default List<FootballMatch> selectByClubId(Long clubId, String season) {
        QueryWrapper<FootballMatch> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("home_team_id", clubId).or().eq("away_team_id", clubId));
        
        if (season != null && !season.isEmpty()) {
            qw.apply("YEAR(start_time) = {0}", season);
        }
        
        qw.orderByDesc("start_time", "id");
        return this.selectList(qw);
    }

    /**
     * 仅用于联赛积分榜：根据比赛时间判断已结束的比赛（避免状态字段不一致问题）
     * 只统计：比赛时间已过 且 比分已录入 的比赛
     */
    @Select("SELECT id, league_id AS leagueId, home_team_id AS homeTeamId, away_team_id AS awayTeamId, "
            + "home_score AS homeScore, away_score AS awayScore, start_time AS startTime "
            + "FROM football_match "
            + "WHERE home_score IS NOT NULL AND away_score IS NOT NULL "
            + "AND start_time <= NOW() "
            + "ORDER BY start_time DESC")
    List<FootballMatch> selectEndedMatchesForStandings();
}