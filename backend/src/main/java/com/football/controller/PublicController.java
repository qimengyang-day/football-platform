package com.football.controller;

import com.football.common.Result;
import com.football.entity.Coach;
import com.football.entity.Player;
import com.football.entity.PlayerInfo;
import com.football.entity.PlatformContact;
import com.football.entity.TeamClub;
import com.football.entity.League;
import com.football.mapper.CoachMapper;
import com.football.mapper.PlatformContactMapper;
import com.football.mapper.PlayerMapper;
import com.football.mapper.PlayerInfoMapper;
import com.football.mapper.TeamClubMapper;
import com.football.mapper.LeagueMapper;
import com.football.utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    @Autowired
    private PlatformContactMapper platformContactMapper;
    
    @Autowired
    private TeamClubMapper teamClubMapper;
    
    @Autowired
    private LeagueMapper leagueMapper;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private PlayerInfoMapper playerInfoMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private RedisUtil redisUtil;

    @GetMapping("/contact")
    public Result<PlatformContact> getContact() {
        // 简化：只取平台联系表第一条配置
        List<PlatformContact> contacts = platformContactMapper.selectList(null);
        if (contacts == null || contacts.isEmpty()) {
            return Result.success(null);
        }
        return Result.success(contacts.get(0));
    }
    
    @GetMapping("/club/list")
    public Result<List<TeamClub>> getClubList() {
        // 先从 Redis 缓存获取
        String cacheKey = "club:list:all";
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取俱乐部列表");
                return Result.success((List<TeamClub>) cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        List<TeamClub> clubs = teamClubMapper.selectList(null);
        
        // 存入 Redis 缓存 (30分钟过期)
        try {
            redisUtil.set(cacheKey, clubs, 1800);
            System.out.println("俱乐部列表已缓存，共 " + (clubs != null ? clubs.size() : 0) + " 个俱乐部");
        } catch (Exception e) {
            System.out.println("Redis 缓存存储失败: " + e.getMessage());
        }
        
        return Result.success(clubs);
    }

    /**
     * 俱乐部名称唯一性校验（注册页用）
     * 返回 true 表示已存在（不可用）
     */
    @GetMapping("/club/check-name")
    public Result<Boolean> checkClubName(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return Result.success(false);
        }
        Long cnt = teamClubMapper.selectCount(new QueryWrapper<TeamClub>().eq("name", name.trim()));
        return Result.success(cnt != null && cnt > 0);
    }
    
    @GetMapping("/league/list")
    public Result<List<League>> getLeagueList() {
        // 先从 Redis 缓存获取
        String cacheKey = "league:list:all";
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取联赛列表");
                return Result.success((List<League>) cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        List<League> leagues = leagueMapper.selectList(null);
        
        // 存入 Redis 缓存 (1小时过期)
        try {
            redisUtil.set(cacheKey, leagues, 3600);
            System.out.println("联赛列表已缓存，共 " + (leagues != null ? leagues.size() : 0) + " 个联赛");
        } catch (Exception e) {
            System.out.println("Redis 缓存存储失败: " + e.getMessage());
        }
        
        return Result.success(leagues);
    }

    @GetMapping("/club/{clubId}/personnel")
    public Result<Object> getClubPersonnel(@PathVariable Long clubId) {
        // 先从 Redis 缓存获取
        String cacheKey = "club:personnel:" + clubId;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取俱乐部 " + clubId + " 人员信息");
                return Result.success((Object) cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        try {
            TeamClub club = teamClubMapper.selectById(clubId);
            if (club == null) {
                return Result.error("俱乐部不存在");
            }

            List<Coach> coaches = coachMapper.selectList(
                    new QueryWrapper<Coach>().eq("club_id", clubId).orderByAsc("id")
            );
            if (coaches == null) coaches = new ArrayList<>();
            // 兼容 team_club 旧字段主教练/翻译
            if ((coaches.isEmpty()) && (club.getHeadCoach() != null || club.getTranslator() != null)) {
                if (club.getHeadCoach() != null && !club.getHeadCoach().trim().isEmpty()) {
                    Coach hc = new Coach();
                    hc.setClubId(clubId);
                    hc.setName(club.getHeadCoach().trim());
                    hc.setPosition("主教练");
                    coaches.add(hc);
                }
                if (club.getTranslator() != null && !club.getTranslator().trim().isEmpty()) {
                    String[] arr = club.getTranslator().split(",");
                    for (String s : arr) {
                        if (s == null || s.trim().isEmpty()) continue;
                        Coach t = new Coach();
                        t.setClubId(clubId);
                        t.setName(s.trim());
                        t.setPosition("翻译");
                        coaches.add(t);
                    }
                }
            }

            List<PlayerInfo> members = playerInfoMapper.selectList(
                    new QueryWrapper<PlayerInfo>().eq("team_id", clubId).orderByAsc("id")
            );
            if (members == null) members = new ArrayList<>();
            List<Player> legacyPlayers;
            try {
                legacyPlayers = playerMapper.selectList(
                        new QueryWrapper<Player>().eq("club_id", clubId).orderByAsc("id")
                );
            } catch (Exception e) {
                legacyPlayers = new ArrayList<>();
            }
            if (legacyPlayers == null) legacyPlayers = new ArrayList<>();

            Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
            groups.put("前锋", new ArrayList<>());
            groups.put("中场", new ArrayList<>());
            groups.put("后卫", new ArrayList<>());
            groups.put("门将", new ArrayList<>());
            groups.put("其他", new ArrayList<>());

            for (PlayerInfo p : members) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", p.getUserId());
                row.put("name", p.getRealName() == null || p.getRealName().trim().isEmpty() ? ("球员#" + p.getUserId()) : p.getRealName());
                row.put("position", p.getPosition());
                row.put("age", p.getAge());
                row.put("nationality", p.getNationality());
                row.put("marketValue", p.getMarketValue());

                String pos = p.getPosition() == null ? "" : p.getPosition();
                String key = "其他";
                if (pos.contains("前锋")) key = "前锋";
                else if (pos.contains("中场")) key = "中场";
                else if (pos.contains("后卫")) key = "后卫";
                else if (pos.contains("门将")) key = "门将";
                groups.get(key).add(row);
            }

            // 兼容 legacy player 表：若 player_info 未覆盖的数据，补充到人员列表中
            for (Player p : legacyPlayers) {
                String key = "其他";
                String pos = p.getPosition() == null ? "" : p.getPosition();
                if (pos.contains("前锋")) key = "前锋";
                else if (pos.contains("中场")) key = "中场";
                else if (pos.contains("后卫")) key = "后卫";
                else if (pos.contains("门将")) key = "门将";

                boolean exists = groups.get(key).stream().anyMatch(r ->
                        String.valueOf(r.getOrDefault("name", "")).equals(String.valueOf(p.getName()))
                );
                if (!exists) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("userId", null);
                    row.put("name", p.getName() == null || p.getName().trim().isEmpty() ? ("球员#" + p.getId()) : p.getName());
                    row.put("position", p.getPosition());
                    row.put("age", p.getAge());
                    row.put("nationality", p.getNationality());
                    row.put("marketValue", p.getMarketValue());
                    groups.get(key).add(row);
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("club", club);
            data.put("coaches", coaches);
            data.put("groups", groups);
            int totalPlayers = groups.values().stream().mapToInt(List::size).sum();
            data.put("playerCount", totalPlayers);
            
            // 存入 Redis 缓存 (30分钟过期)
            try {
                redisUtil.set(cacheKey, data, 1800);
                System.out.println("俱乐部 " + clubId + " 人员信息已缓存");
            } catch (Exception e) {
                System.out.println("Redis 缓存存储失败: " + e.getMessage());
            }
            
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取俱乐部人员失败：" + e.getMessage());
        }
    }
}

