package com.football.controller;

import com.football.common.Result;
import com.football.entity.FootballMatch;
import com.football.entity.League;
import com.football.entity.TeamClub;
import com.football.mapper.FootballMatchMapper;
import com.football.mapper.LeagueMapper;
import com.football.mapper.TeamClubMapper;
import com.football.utils.RedisUtil;
import com.football.utils.MatchStatusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/match")
public class MatchPublicController {
    @Autowired
    private FootballMatchMapper footballMatchMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private MatchStatusUtil matchStatusUtil;

    @Autowired
    private TeamClubMapper teamClubMapper;

    @Autowired
    private LeagueMapper leagueMapper;

    private String normalizeStatus(String status) {
        if (status == null) return null;
        String s = status.trim();
        if (s.isEmpty()) return s;
        if ("报名中".equals(s) || "待比赛".equals(s) || "未开始".equals(s) || "UPCOMING".equalsIgnoreCase(s)) return "REGISTERING";
        if ("进行中".equals(s) || "ONGOING".equalsIgnoreCase(s)) return "ONGOING";
        if ("已结束".equals(s) || "COMPLETED".equalsIgnoreCase(s) || "FINISHED".equalsIgnoreCase(s)) return "ENDED";
        return s;
    }

    private void enrichTeamsAndTitle(FootballMatch match) {
        if (match == null) return;
        Long homeId = match.getHomeTeamId();
        Long awayId = match.getAwayTeamId();

        String homeName = null;
        String awayName = null;
        if (homeId != null) {
            TeamClub home = teamClubMapper.selectById(homeId);
            homeName = home != null ? home.getName() : null;
        }
        if (awayId != null) {
            TeamClub away = teamClubMapper.selectById(awayId);
            awayName = away != null ? away.getName() : null;
        }

        match.setHomeTeamName(homeName);
        match.setAwayTeamName(awayName);
        if (homeName != null && awayName != null) {
            match.setTitle(homeName + "VS" + awayName);
        }

        // 统一补齐联赛名称、赛果等字段，确保球迷端与管理员端一致
        if (match.getLeagueId() != null) {
            League league = leagueMapper.selectById(match.getLeagueId());
            match.setLeagueName(league == null ? null : league.getName());
        }

        // 兼容前端 matchTime 字段
        if (match.getStartTime() != null) {
            match.setMatchTime(match.getStartTime());
        }

        // 使用 MatchStatusUtil 计算实时状态（带缓存）
        Date matchStartTime = match.getStartTime() != null ? match.getStartTime() : match.getMatchTime();
        String calculatedStatus = matchStatusUtil.getMatchStatusWithCache(match.getId(), matchStartTime);
        match.setStatus(calculatedStatus);

        Integer hs = match.getHomeScore();
        Integer as = match.getAwayScore();
        
        // 根据实时状态设置赛果
        if ("REGISTERING".equals(calculatedStatus)) {
            match.setMatchResult("待比赛");
            return;
        }
        if (hs == null || as == null) {
            match.setMatchResult("ONGOING".equals(calculatedStatus) ? "进行中" : "待录入比分");
            return;
        }
        if (hs > as) {
            match.setWinnerTeamName(homeName);
            match.setMatchResult(hs + ":" + as + ("ONGOING".equals(calculatedStatus) ? " 进行中(主队领先)" : " 主队胜"));
        } else if (hs < as) {
            match.setWinnerTeamName(awayName);
            match.setMatchResult(hs + ":" + as + ("ONGOING".equals(calculatedStatus) ? " 进行中(客队领先)" : " 客队胜"));
        } else {
            match.setWinnerTeamName("平局");
            match.setMatchResult(hs + ":" + as + ("ONGOING".equals(calculatedStatus) ? " 进行中" : " 平局"));
        }
    }

    @GetMapping("/list")
    public Result<List<FootballMatch>> getMatchList() {
        // 先从 Redis 缓存获取
        String cacheKey = "match:list:all";
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取比赛列表");
                return Result.success((List<FootballMatch>) cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        List<FootballMatch> matches = footballMatchMapper.selectList(null);
        if (matches != null) {
            for (FootballMatch m : matches) {
                enrichTeamsAndTitle(m);
            }
        }
        
        // 存入 Redis 缓存 (5分钟过期)
        try {
            redisUtil.set(cacheKey, matches, 300);
            System.out.println("比赛列表已缓存，共 " + (matches != null ? matches.size() : 0) + " 场");
        } catch (Exception e) {
            System.out.println("Redis 缓存存储失败: " + e.getMessage());
        }
        
        return Result.success(matches);
    }

    @GetMapping("/hot/{id}")
    public Result<Long> getMatchHot(@PathVariable Long id) {
        FootballMatch match = footballMatchMapper.selectById(id);
        if (match != null) {
            long viewCount = match.getViewCount() + 1;
            match.setViewCount(viewCount);
            footballMatchMapper.updateById(match);
            
            // 清除比赛详情缓存，确保下次访问获取最新热度
            try {
                redisUtil.delete("match:detail:" + id);
            } catch (Exception e) {
                System.err.println("清除比赛详情缓存失败: " + e.getMessage());
            }
            
            return Result.success(viewCount);
        }
        return Result.error("赛事不存在");
    }

    @GetMapping("/{id}")
    public Result<FootballMatch> getMatchDetail(@PathVariable Long id) {
        // 先从 Redis 缓存获取
        String cacheKey = "match:detail:" + id;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取比赛详情: matchId=" + id);
                return Result.success((FootballMatch) cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        FootballMatch match = footballMatchMapper.selectById(id);
        if (match == null) {
            return Result.error("赛事不存在");
        }
        enrichTeamsAndTitle(match);
        
        // 存入 Redis 缓存 (5分钟过期)
        try {
            redisUtil.set(cacheKey, match, 300);
            System.out.println("比赛详情已缓存: matchId=" + id);
        } catch (Exception e) {
            System.out.println("Redis 缓存存储失败: " + e.getMessage());
        }
        
        return Result.success(match);
    }
}