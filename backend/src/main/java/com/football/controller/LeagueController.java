package com.football.controller;

import com.football.common.Result;
import com.football.entity.ClubLeagueRelation;
import com.football.entity.FootballMatch;
import com.football.entity.League;
import com.football.entity.TeamClub;
import com.football.mapper.ClubLeagueRelationMapper;
import com.football.mapper.FootballMatchMapper;
import com.football.mapper.TeamClubMapper;
import com.football.service.LeagueService;
import com.football.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/league")
public class LeagueController {
    @Autowired
    private LeagueService leagueService;
    @Autowired
    private ClubLeagueRelationMapper clubLeagueRelationMapper;
    @Autowired
    private FootballMatchMapper footballMatchMapper;
    @Autowired
    private TeamClubMapper teamClubMapper;
    @Autowired
    private RedisUtil redisUtil;

    /**
     * 获取所有联赛列表（不分页）
     */
    @GetMapping("/list")
    public Result<List<League>> getLeagueList() {
        try {
            System.out.println("Getting all leagues...");
            List<League> leagues = leagueService.getAllLeagues();
            System.out.println("Leagues found: " + (leagues != null ? leagues.size() : "null"));
            return Result.success(leagues);
        } catch (Exception e) {
            System.out.println("Error getting leagues: " + e.getMessage());
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取联赛下的俱乐部
     */
    @GetMapping("/{leagueId}/clubs")
    public Result<List<TeamClub>> getLeagueClubs(@PathVariable Long leagueId) {
        try {
            System.out.println("Getting clubs for league: " + leagueId);
            List<TeamClub> clubs = leagueService.getClubsByLeagueId(leagueId);
            System.out.println("Clubs found: " + (clubs != null ? clubs.size() : "null"));
            return Result.success(clubs);
        } catch (Exception e) {
            System.out.println("Error getting clubs: " + e.getMessage());
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    /**
     * 联赛积分榜（按常见足球规则）
     * 排序：积分(3/1/0) > 净胜球 > 进球数 > 胜场 > 名称
     */
    @GetMapping("/{leagueId}/standings")
    public Result<List<Map<String, Object>>> getLeagueStandings(@PathVariable Long leagueId) {
        // 先从 Redis 缓存获取
        String cacheKey = "league:standings:" + leagueId;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取联赛 " + leagueId + " 积分榜");
                return Result.success((List<Map<String, Object>>) cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        try {
            List<ClubLeagueRelation> relations = clubLeagueRelationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ClubLeagueRelation>()
                            .eq("league_id", leagueId)
            );
            Map<Long, Map<String, Object>> table = new LinkedHashMap<>();
            for (ClubLeagueRelation r : relations) {
                TeamClub c = teamClubMapper.selectById(r.getClubId());
                if (c == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("clubId", c.getId());
                row.put("teamName", c.getName());
                row.put("teamLogo", c.getLogo());
                row.put("played", 0);
                row.put("wins", 0);
                row.put("draws", 0);
                row.put("losses", 0);
                row.put("goalsFor", 0);
                row.put("goalsAgainst", 0);
                row.put("goalDiff", 0);
                row.put("points", 0);
                table.put(c.getId(), row);
            }

            if (table.isEmpty()) return Result.success(new ArrayList<>());

            // 兜底：历史数据可能没有 league_id（为避免全是0），这里先取“已结束”的所有比赛，
            // 然后在循环里用 club_league_relation 判断该比赛是否属于当前 leagueId。
            // 用 mapper 的强制别名查询，避免因映射策略导致 homeScore/awayScore 取到 null。
            List<FootballMatch> matches = footballMatchMapper.selectEndedMatchesForStandings();

            for (FootballMatch m : matches) {
                if (m.getHomeTeamId() == null || m.getAwayTeamId() == null) continue;
                if (m.getHomeScore() == null || m.getAwayScore() == null) continue;

                // 判断该比赛是否属于当前联赛
                boolean belongsToLeague;
                if (m.getLeagueId() != null) {
                    belongsToLeague = leagueId.equals(m.getLeagueId());
                } else {
                    // league_id 为空：根据双方是否都隶属于该联赛兜底
                    Long homeId = m.getHomeTeamId();
                    Long awayId = m.getAwayTeamId();
                    belongsToLeague = table.containsKey(homeId) && table.containsKey(awayId);
                }
                if (!belongsToLeague) continue;

                Map<String, Object> home = table.get(m.getHomeTeamId());
                Map<String, Object> away = table.get(m.getAwayTeamId());
                if (home == null || away == null) continue;

                int hs = m.getHomeScore();
                int as = m.getAwayScore();

                home.put("played", ((int) home.get("played")) + 1);
                away.put("played", ((int) away.get("played")) + 1);
                home.put("goalsFor", ((int) home.get("goalsFor")) + hs);
                home.put("goalsAgainst", ((int) home.get("goalsAgainst")) + as);
                away.put("goalsFor", ((int) away.get("goalsFor")) + as);
                away.put("goalsAgainst", ((int) away.get("goalsAgainst")) + hs);

                if (hs > as) {
                    home.put("wins", ((int) home.get("wins")) + 1);
                    away.put("losses", ((int) away.get("losses")) + 1);
                    home.put("points", ((int) home.get("points")) + 3);
                } else if (hs < as) {
                    away.put("wins", ((int) away.get("wins")) + 1);
                    home.put("losses", ((int) home.get("losses")) + 1);
                    away.put("points", ((int) away.get("points")) + 3);
                } else {
                    home.put("draws", ((int) home.get("draws")) + 1);
                    away.put("draws", ((int) away.get("draws")) + 1);
                    home.put("points", ((int) home.get("points")) + 1);
                    away.put("points", ((int) away.get("points")) + 1);
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>(table.values());
            for (Map<String, Object> row : rows) {
                int goalsFor = row.get("goalsFor") != null ? (int) row.get("goalsFor") : 0;
                int goalsAgainst = row.get("goalsAgainst") != null ? (int) row.get("goalsAgainst") : 0;
                int gd = goalsFor - goalsAgainst;
                row.put("goalDiff", gd);
            }

            // 排序前打印调试信息
            System.out.println("排序前的数据:");
            for (Map<String, Object> row : rows) {
                System.out.println("球队：" + row.get("teamName") + ", 积分：" + row.get("points") + ", 净胜球：" + row.get("goalDiff") + ", 进球：" + row.get("goalsFor") + ", 胜场：" + row.get("wins"));
            }

            rows.sort((r1, r2) -> {
                // 第一优先级：积分（降序）
                int points1 = (Integer) r1.get("points");
                int points2 = (Integer) r2.get("points");
                if (points1 != points2) {
                    return Integer.compare(points2, points1); // 高分在前
                }
                
                // 第二优先级：净胜球（降序）
                int gd1 = (Integer) r1.get("goalDiff");
                int gd2 = (Integer) r2.get("goalDiff");
                if (gd1 != gd2) {
                    return Integer.compare(gd2, gd1); // 净胜球多在前
                }
                
                // 第三优先级：总进球数（降序）
                int gf1 = (Integer) r1.get("goalsFor");
                int gf2 = (Integer) r2.get("goalsFor");
                if (gf1 != gf2) {
                    return Integer.compare(gf2, gf1); // 进球多在前
                }
                
                // 第四优先级：胜场（降序）
                int wins1 = (Integer) r1.get("wins");
                int wins2 = (Integer) r2.get("wins");
                if (wins1 != wins2) {
                    return Integer.compare(wins2, wins1); // 胜场多在前
                }
                
                // 第五优先级：球队名称（升序）
                String name1 = String.valueOf(r1.get("teamName"));
                String name2 = String.valueOf(r2.get("teamName"));
                return name1.compareTo(name2);
            });
            
            // 排序后打印调试信息
            System.out.println("排序后的数据:");
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                System.out.println("排名：" + (i+1) + ", 球队：" + row.get("teamName") + ", 积分：" + row.get("points"));
            }
            
            for (int i = 0; i < rows.size(); i++) rows.get(i).put("rank", i + 1);
            
            // 存入 Redis 缓存 (10分钟过期)
            try {
                redisUtil.set(cacheKey, rows, 600);
                System.out.println("联赛 " + leagueId + " 积分榜已缓存，共 " + rows.size() + " 支球队");
            } catch (Exception e) {
                System.out.println("Redis 缓存存储失败: " + e.getMessage());
            }
            
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error("获取联赛积分榜失败：" + e.getMessage());
        }
    }
}
