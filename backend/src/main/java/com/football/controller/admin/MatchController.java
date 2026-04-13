package com.football.controller.admin;

import com.football.common.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.ClubLeagueRelation;
import com.football.entity.League;
import com.football.entity.TeamClub;
import com.football.entity.FootballMatch;
import com.football.mapper.ClubLeagueRelationMapper;
import com.football.mapper.FootballMatchMapper;
import com.football.mapper.LeagueMapper;
import com.football.mapper.TeamClubMapper;
import com.football.utils.RedisUtil;
import com.football.utils.MatchStatusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/match")
public class MatchController {
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
    @Autowired
    private ClubLeagueRelationMapper clubLeagueRelationMapper;

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

    private boolean clubInLeague(Long clubId, Long leagueId) {
        if (clubId == null || leagueId == null) return false;
        Long count = clubLeagueRelationMapper.selectCount(
                new QueryWrapper<ClubLeagueRelation>()
                        .eq("club_id", clubId)
                        .eq("league_id", leagueId)
        );
        return count != null && count > 0;
    }

    private Result<Boolean> validateAndNormalize(FootballMatch match) {
        if (match.getLeagueId() == null) return Result.error("请选择所属联赛");
        if (match.getHomeTeamId() == null || match.getAwayTeamId() == null) {
            return Result.error("请选择主队与客队");
        }
        if (match.getHomeTeamId().equals(match.getAwayTeamId())) {
            return Result.error("主队和客队不能相同");
        }
        if (!clubInLeague(match.getHomeTeamId(), match.getLeagueId()) || !clubInLeague(match.getAwayTeamId(), match.getLeagueId())) {
            return Result.error("对战双方必须属于所选联赛");
        }
        
        // 使用 MatchStatusUtil 计算实时状态
        Date matchStartTime = match.getStartTime() != null ? match.getStartTime() : match.getMatchTime();
        String calculatedStatus = matchStatusUtil.calculateMatchStatus(matchStartTime);
        match.setStatus(calculatedStatus);
        
        // 比分录入校验：待比赛状态不可录入比分
        if ("REGISTERING".equals(calculatedStatus)) {
            if (match.getHomeScore() != null || match.getAwayScore() != null) {
                return Result.error("比赛尚未开始，不可录入比分");
            }
            match.setHomeScore(null);
            match.setAwayScore(null);
        } else {
            // 进行中或已结束，允许录入比分
            if (match.getHomeScore() == null) match.setHomeScore(0);
            if (match.getAwayScore() == null) match.setAwayScore(0);
        }
        return null;
    }

    @PostMapping
    public Result<Boolean> createMatch(@RequestBody FootballMatch match) {
        Result<Boolean> validate = validateAndNormalize(match);
        if (validate != null) return validate;
        enrichTeamsAndTitle(match);
        footballMatchMapper.insert(match);
        
        // 清除比赛列表缓存
        try {
            redisUtil.delete("match:list:all");
            System.out.println("已清除比赛列表缓存");
        } catch (Exception e) {
            System.out.println("清除缓存失败: " + e.getMessage());
        }
        
        return Result.success(true);
    }

    @GetMapping("/list")
    public Result<Object> getMatchList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String search
    ) {
        Page<FootballMatch> page = new Page<>(pageNum, pageSize);
        QueryWrapper<FootballMatch> qw = new QueryWrapper<>();
        if (search != null && !search.trim().isEmpty()) {
            qw.like("title", search.trim());
        }
        qw.orderByDesc("id");
        footballMatchMapper.selectPage(page, qw);

        for (FootballMatch m : page.getRecords()) {
            enrichTeamsAndTitle(m);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", page.getRecords());
        data.put("total", page.getTotal());
        return Result.success(data);
    }

    @PutMapping("/schedule")
    public Result<Boolean> scheduleMatch(@RequestBody FootballMatch match) {
        Result<Boolean> validate = validateAndNormalize(match);
        if (validate != null) return validate;
        enrichTeamsAndTitle(match);
        footballMatchMapper.updateById(match);
        
        // 清除比赛列表缓存、状态缓存和相关联赛积分榜缓存
        clearMatchAndStandingsCache(match.getLeagueId(), match.getId());
        
        return Result.success(true);
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateMatch(@PathVariable Long id, @RequestBody FootballMatch match) {
        match.setId(id);
        Result<Boolean> validate = validateAndNormalize(match);
        if (validate != null) return validate;
        enrichTeamsAndTitle(match);
        int result = footballMatchMapper.updateById(match);
        
        // 清除比赛列表缓存、状态缓存和相关联赛积分榜缓存
        clearMatchAndStandingsCache(match.getLeagueId(), match.getId());
        
        return Result.success(result > 0);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteMatch(@PathVariable Long id) {
        // 先获取比赛信息，以便清除对应的联赛缓存
        FootballMatch match = footballMatchMapper.selectById(id);
        Long leagueId = match != null ? match.getLeagueId() : null;
        int result = footballMatchMapper.deleteById(id);
        
        if (result > 0) {
            // 清除比赛列表缓存、状态缓存和相关联赛积分榜缓存
            clearMatchAndStandingsCache(leagueId, id);
        }
        
        return Result.success(result > 0);
    }
    
    /**
     * 清除比赛列表、状态缓存和联赛积分榜缓存
     * @param leagueId 联赛ID
     * @param matchId 比赛ID（可为 null）
     */
    private void clearMatchAndStandingsCache(Long leagueId, Long matchId) {
        try {
            // 清除比赛列表缓存
            redisUtil.delete("match:list:all");
            System.out.println("已清除比赛列表缓存");
            
            // 清除单场比赛状态缓存
            if (matchId != null) {
                matchStatusUtil.clearMatchStatusCache(matchId);
            }
            
            // 清除相关联赛的积分榜缓存
            if (leagueId != null) {
                redisUtil.delete("league:standings:" + leagueId);
                System.out.println("已清除联赛 " + leagueId + " 积分榜缓存");
            }
        } catch (Exception e) {
            System.out.println("清除缓存失败: " + e.getMessage());
        }
    }
    
    /**
     * 录入比赛比分（带时间校验）
     */
    @PutMapping("/{id}/score")
    public Result<Boolean> updateMatchScore(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            FootballMatch match = footballMatchMapper.selectById(id);
            if (match == null) {
                return Result.error("比赛不存在");
            }
            
            // 时间校验：比赛尚未开始，不可录入比分
            Date matchStartTime = match.getStartTime() != null ? match.getStartTime() : match.getMatchTime();
            if (!matchStatusUtil.canInputScore(matchStartTime)) {
                return Result.error("比赛尚未开始，不可录入比分");
            }
            
            // 获取比分
            Integer homeScore = body.get("homeScore") != null ? Integer.parseInt(body.get("homeScore").toString()) : null;
            Integer awayScore = body.get("awayScore") != null ? Integer.parseInt(body.get("awayScore").toString()) : null;
            
            if (homeScore == null || awayScore == null) {
                return Result.error("请输入完整比分");
            }
            
            if (homeScore < 0 || awayScore < 0) {
                return Result.error("比分不能为负数");
            }
            
            // 更新比分
            match.setHomeScore(homeScore);
            match.setAwayScore(awayScore);
            
            // 重新计算状态（基于当前时间）
            String newStatus = matchStatusUtil.calculateMatchStatus(matchStartTime);
            match.setStatus(newStatus);
            
            int result = footballMatchMapper.updateById(match);
            
            // 清除缓存
            clearMatchAndStandingsCache(match.getLeagueId(), match.getId());
            
            return Result.success(result > 0);
        } catch (Exception e) {
            return Result.error("录入比分失败：" + e.getMessage());
        }
    }
}