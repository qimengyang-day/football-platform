package com.football.controller.club;

import com.football.common.Result;
import com.football.entity.TeamClub;
import com.football.entity.MatchRegistration;
import com.football.entity.Coach;
import com.football.entity.Player;
import com.football.entity.PlayerApplication;
import com.football.entity.FootballMatch;
import com.football.entity.PlayerInfo;
import com.football.entity.ClubLeagueRelation;
import com.football.entity.ClubSponsor;
import com.football.entity.League;
import com.football.mapper.TeamClubMapper;
import com.football.mapper.MatchRegistrationMapper;
import com.football.mapper.CoachMapper;
import com.football.mapper.PlayerMapper;
import com.football.mapper.PlayerApplicationMapper;
import com.football.mapper.FootballMatchMapper;
import com.football.mapper.PlayerInfoMapper;
import com.football.mapper.ClubLeagueRelationMapper;
import com.football.mapper.ClubSponsorMapper;
import com.football.mapper.LeagueMapper;
import com.football.service.LeagueService;
import com.football.utils.JwtUtil;
import com.football.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/club")
public class ClubController {
    @Autowired
    private TeamClubMapper teamClubMapper;

    @Autowired
    private MatchRegistrationMapper matchRegistrationMapper;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private PlayerApplicationMapper playerApplicationMapper;

    @Autowired
    private FootballMatchMapper footballMatchMapper;
    @Autowired
    private PlayerInfoMapper playerInfoMapper;

    @Autowired
    private ClubLeagueRelationMapper clubLeagueRelationMapper;

    @Autowired
    private ClubSponsorMapper clubSponsorMapper;

    @Autowired
    private LeagueService leagueService;

    @Autowired
    private LeagueMapper leagueMapper;

    @Autowired
    private RedisUtil redisUtil;

    private TeamClub getManagerClub(Long managerId) {
        List<TeamClub> clubs = teamClubMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamClub>()
                        .eq("manager_id", managerId)
                        .orderByAsc("id")
        );
        return (clubs == null || clubs.isEmpty()) ? null : clubs.get(0);
    }

    @GetMapping("/list")
    public Result<List<TeamClub>> getClubList() {
        List<TeamClub> clubs = teamClubMapper.selectList(null);
        return Result.success(clubs);
    }

    @PostMapping("/team")
    public Result<Boolean> createTeam(@RequestBody TeamClub team) {
        teamClubMapper.insert(team);
        return Result.success(true);
    }

    @PostMapping("/match/register/{id}")
    public Result<Boolean> registerMatch(@PathVariable Long id, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            MatchRegistration registration = new MatchRegistration();
            registration.setMatchId(id);
            registration.setTeamClubId(club.getId());
            registration.setUserId(userId);
            registration.setStatus("REGISTERED");

            matchRegistrationMapper.insert(registration);
        return Result.success(true);
        } catch (Exception e) {
            // UNIQUE KEY 冲突等情况直接提示即可
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/team/import")
    public Result<Boolean> importPlayers() {
        // 实现批量导入球员逻辑
        return Result.success(true);
    }

    @GetMapping("/info")
    public Result<TeamClub> getMyClubInfo(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long userId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            
            // 尝试从 Redis 缓存获取
            String cacheKey = "club:info:" + userId;
            try {
                Object cached = redisUtil.get(cacheKey);
                if (cached != null) {
                    System.out.println("从缓存获取俱乐部信息: userId=" + userId);
                    return Result.success((TeamClub) cached);
                }
            } catch (Exception e) {
                System.err.println("Redis 缓存读取失败，降级到数据库查询: " + e.getMessage());
            }
            
            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            // 统一把 sponsor/translator/headCoach 从规范化表读取，保证“多值”一致
            List<ClubSponsor> sponsors = clubSponsorMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ClubSponsor>()
                            .eq("club_id", club.getId())
            );
            if (sponsors != null && !sponsors.isEmpty()) {
                String joined = sponsors.stream()
                        .map(ClubSponsor::getSponsorName)
                        .filter(s -> s != null && !s.trim().isEmpty())
                        .collect(java.util.stream.Collectors.joining(","));
                club.setSponsor(joined);
            } else {
                // 兼容兜底：如果历史数据还没迁移到 club_sponsor，就退回使用 team_club.sponsor
                String legacy = club.getSponsor();
                club.setSponsor(legacy != null ? legacy : "");
            }

            // headCoach / translator 从 coach 表推导
            List<Coach> coachList = coachMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Coach>()
                            .eq("club_id", club.getId())
            );
            if (coachList != null) {
                String headName = coachList.stream()
                        .filter(c -> c.getPosition() != null && c.getPosition().contains("主教练"))
                        .map(Coach::getName)
                        .findFirst()
                        .orElse(club.getHeadCoach());
                club.setHeadCoach(headName);

                // 翻译允许多个
                String translatorNames = coachList.stream()
                        .filter(c -> c.getPosition() != null && c.getPosition().contains("翻译"))
                        .map(Coach::getName)
                        .filter(n -> n != null && !n.trim().isEmpty())
                        .collect(java.util.stream.Collectors.joining(","));
                // 兜底：若 coach 表还没迁移，使用 team_club.translator 字段
                if (translatorNames != null && !translatorNames.trim().isEmpty()) {
                    club.setTranslator(translatorNames);
                } else {
                    String legacyTranslator = club.getTranslator();
                    club.setTranslator(legacyTranslator != null ? legacyTranslator : "");
                }

                // 主教练价值从 coach.salary 读取（若存在）
                Coach headCoach = coachList.stream()
                        .filter(c -> c.getPosition() != null && c.getPosition().contains("主教练"))
                        .findFirst()
                        .orElse(null);
                if (headCoach != null && headCoach.getSalary() != null) {
                    club.setCoachValue(java.math.BigDecimal.valueOf(headCoach.getSalary()));
                }
            }
            
            // 存入 Redis 缓存 (10分钟过期)
            try {
                redisUtil.set(cacheKey, club, 600);
                System.out.println("俱乐部信息已缓存: userId=" + userId);
            } catch (Exception e) {
                System.err.println("Redis 缓存存储失败，不影响业务: " + e.getMessage());
            }

            return Result.success(club);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info/summary")
    public Result<Object> getMyClubSummary(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long userId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            TeamClub club = getManagerClub(userId);
            if (club == null) return Result.error("未找到你的俱乐部信息");

            // 保证“主教练价值”来自 coach 表（coach.salary），避免 team_club.coach_value 脏数据
            // 注意：单元测试里未必 mock coachMapper，因此这里做空保护。
            if (coachMapper != null) {
                List<Coach> coachList = coachMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Coach>()
                                .eq("club_id", club.getId())
                );
                if (coachList != null) {
                    Coach head = coachList.stream()
                            .filter(c -> c.getPosition() != null && c.getPosition().contains("主教练"))
                            .findFirst()
                            .orElse(null);
                    if (head != null && head.getSalary() != null) {
                        club.setCoachValue(java.math.BigDecimal.valueOf(head.getSalary()));
                    }
                }
            }

            List<PlayerInfo> members = playerInfoMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PlayerInfo>()
                            .eq("team_id", club.getId())
            );

            double totalValue = club.getCoachValue() == null ? 0 : club.getCoachValue().doubleValue();
            for (PlayerInfo pi : members) {
                if (pi.getMarketValue() != null) totalValue += pi.getMarketValue();
            }

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("club", club);
            data.put("playerCount", members.size());
            data.put("teamCount", 1);
            data.put("members", members);
            data.put("totalValue", totalValue);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/info")
    public Result<Boolean> updateMyClubInfo(@RequestBody TeamClub payload, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long userId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            if (payload.getName() != null) club.setName(payload.getName());
            if (payload.getDescription() != null) club.setDescription(payload.getDescription());
            if (payload.getLogo() != null) club.setLogo(payload.getLogo());

            // sponsor：写入 club_sponsor（多值）并同步到 team_club.sponsor（逗号串兼容前端）
            if (payload.getSponsor() != null) {
                String sponsorRaw = payload.getSponsor();
                Set<String> sponsorSet = new HashSet<>();
                for (String s : sponsorRaw.split(",")) {
                    if (s != null) {
                        String t = s.trim();
                        if (!t.isEmpty()) sponsorSet.add(t);
                    }
                }
                clubSponsorMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ClubSponsor>()
                        .eq("club_id", club.getId()));
                for (String name : sponsorSet) {
                    ClubSponsor cs = new ClubSponsor();
                    cs.setClubId(club.getId());
                    cs.setSponsorName(name);
                    clubSponsorMapper.insert(cs);
                }
                club.setSponsor(String.join(",", sponsorSet));
            }

            // translator：写入 coach 表 position='翻译'（允许多个）
            if (payload.getTranslator() != null) {
                String translatorRaw = payload.getTranslator();
                Set<String> translatorSet = new HashSet<>();
                for (String s : translatorRaw.split(",")) {
                    if (s != null) {
                        String t = s.trim();
                        if (!t.isEmpty()) translatorSet.add(t);
                    }
                }

                // 删除旧翻译教练
                coachMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Coach>()
                        .eq("club_id", club.getId())
                        .like("position", "翻译")
                );

                // 插入新翻译教练
                for (String name : translatorSet) {
                    Coach c = new Coach();
                    c.setClubId(club.getId());
                    c.setName(name);
                    c.setPosition("翻译");
                    coachMapper.insert(c);
                }
                club.setTranslator(String.join(",", translatorSet));
            }

            // headCoach：写入 coach 表 position='主教练'（唯一制）
            if (payload.getHeadCoach() != null) {
                String head = payload.getHeadCoach().trim();
                if (!head.isEmpty()) {
                    // 删除旧主教练（如存在）
                    coachMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Coach>()
                            .eq("club_id", club.getId())
                            .like("position", "主教练")
                    );
                    Coach c = new Coach();
                    c.setClubId(club.getId());
                    c.setName(head);
                    c.setPosition("主教练");
                    // salary/contractEndDate 如前端没有传，这里不强制；保持 team_club.coachValue 即可
                    c.setSalary(club.getCoachValue() == null ? 0 : club.getCoachValue().doubleValue());
                    coachMapper.insert(c);
                    club.setHeadCoach(head);
                }
            }

            teamClubMapper.updateById(club);
            
            // 清除缓存
            try {
                redisUtil.delete("club:info:" + userId);
                System.out.println("俱乐部信息缓存已清除: userId=" + userId);
            } catch (Exception e) {
                System.err.println("Redis 缓存清除失败，不影响业务: " + e.getMessage());
            }
            
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/team/captain")
    public Result<Boolean> setCaptain(@RequestParam Long playerId) {
        // 实现设置队长逻辑
        return Result.success(true);
    }

    // 主教练管理
    @GetMapping("/coaches")
    public Result<List<Coach>> getCoaches(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            // 避免调用未提供 SQL 的自定义 mapper 方法：使用通用 selectList
            List<Coach> coaches = coachMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Coach>()
                            .eq("club_id", club.getId())
                            .orderByAsc("id")
            );
            if (coaches == null) coaches = new ArrayList<>();

            // 兼容旧数据：若 team_club 里的主教练/翻译没有同步到 coach 表，也补齐返回给前端
            java.util.Set<String> existKey = new java.util.HashSet<>();
            for (Coach c : coaches) {
                if (c == null) continue;
                String k = String.valueOf(c.getPosition()) + "|" + String.valueOf(c.getName());
                existKey.add(k);
            }

            // 主教练（唯一）
            String legacyHead = club.getHeadCoach();
            if (legacyHead != null && !legacyHead.trim().isEmpty()) {
                String pos = "主教练";
                String name = legacyHead.trim();
                String k = pos + "|" + name;
                if (!existKey.contains(k)) {
                    Coach c = new Coach();
                    c.setClubId(club.getId());
                    c.setPosition(pos);
                    c.setName(name);
                    c.setSalary(club.getCoachValue() == null ? 0 : club.getCoachValue().doubleValue());
                    coaches.add(c);
                    existKey.add(k);
                }
            }

            // 翻译（多值）
            String legacyTranslatorRaw = club.getTranslator();
            if (legacyTranslatorRaw != null && !legacyTranslatorRaw.trim().isEmpty()) {
                String[] parts = legacyTranslatorRaw.split(",");
                for (String part : parts) {
                    if (part == null) continue;
                    String name = part.trim();
                    if (name.isEmpty()) continue;
                    String pos = "翻译";
                    String k = pos + "|" + name;
                    if (!existKey.contains(k)) {
                        Coach c = new Coach();
                        c.setClubId(club.getId());
                        c.setPosition(pos);
                        c.setName(name);
                        c.setSalary(0.0);
                        coaches.add(c);
                        existKey.add(k);
                    }
                }
            }

            return Result.success(coaches);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/coaches")
    public Result<Boolean> addCoach(@RequestBody Coach coach, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            // 主教练唯一制：同一俱乐部只能存在 1 名主教练
            if (coach.getPosition() != null && coach.getPosition().contains("主教练")) {
                Long count = coachMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Coach>()
                                .eq("club_id", club.getId())
                                .like("position", "主教练")
                );
                if (count != null && count > 0) {
                    return Result.error("该俱乐部已存在主教练，无法再次添加");
                }
            }

            coach.setClubId(club.getId());
            coachMapper.insert(coach);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/coaches/{id}")
    public Result<Boolean> updateCoach(@PathVariable Long id, @RequestBody Coach coach, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            coach.setId(id);

            // 若更新为主教练，则校验唯一制
            if (coach.getPosition() != null && coach.getPosition().contains("主教练")) {
                Long count = coachMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Coach>()
                                .eq("club_id", club.getId())
                                .like("position", "主教练")
                                .ne("id", id)
                );
                if (count != null && count > 0) {
                    return Result.error("该俱乐部已存在主教练，无法修改为主教练");
                }
            }

            coach.setClubId(club.getId());
            coachMapper.updateById(coach);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 自由身球员列表（给俱乐部直接添加用）
    @GetMapping("/free-agents")
    public Result<List<PlayerInfo>> getFreeAgents(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            List<PlayerInfo> list = playerInfoMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PlayerInfo>()
                            .eq("is_free_agent", 1)
                            .eq("status", "自由身")
                            .orderByDesc("id")
            );
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 直接把自由身球员加入俱乐部（用于“俱乐部人员管理-添加球员”）
    @PostMapping("/roster/players/add")
    public Result<Boolean> addFreeAgentToClub(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long managerId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(managerId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            Object raw = body.get("playerUserId");
            if (raw == null) {
                return Result.error("缺少参数 playerUserId");
            }
            Long playerUserId = Long.valueOf(raw.toString());

            PlayerInfo pi = playerInfoMapper.selectByUserId(playerUserId);
            if (pi == null) {
                return Result.error("球员档案不存在，请先在球员端创建自由身档案");
            }

            // 验证：只能添加自由身球员
            if (pi.getIsFreeAgent() == null || pi.getIsFreeAgent() != 1 || pi.getStatus() == null || !"自由身".equals(pi.getStatus())) {
                String currentClubName = "未知俱乐部";
                if (pi.getTeamId() != null) {
                    TeamClub current = teamClubMapper.selectById(pi.getTeamId());
                    if (current != null) currentClubName = current.getName();
                }
                return Result.error("该球员当前隶属于[" + currentClubName + "]，无法添加");
            }

            // 加入俱乐部：更新 player_info（club 端展示基于 player_info.team_id）
            pi.setTeamId(club.getId());
            pi.setStatus("俱乐部成员");
            pi.setIsFreeAgent(0);
            pi.setJoinStatus("已审核");
            pi.setApplyTeamId(null);
            pi.setApplyReason(null);
            pi.setAdminRemark(null);
            pi.setClubRemark(null);

            playerInfoMapper.updateById(pi);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 俱乐部角色：把“本俱乐部”加入某联赛
    @PostMapping("/league/club/add")
    public Result<Boolean> addThisClubToLeague(@RequestParam Long leagueId, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long managerId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(managerId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            // 避免重复关联
            ClubLeagueRelation exist = clubLeagueRelationMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ClubLeagueRelation>()
                            .eq("club_id", club.getId())
                            .eq("league_id", leagueId)
            );
            if (exist != null) {
                return Result.success(true);
            }

            boolean success = leagueService.addClubToLeague(club.getId(), leagueId);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/coaches/{id}")
    public Result<Boolean> deleteCoach(@PathVariable Long id) {
        try {
            coachMapper.deleteById(id);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 球员管理
    @GetMapping("/players")
    public Result<List<Player>> getPlayers(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            List<Player> players = playerMapper.selectByClubId(club.getId());
            return Result.success(players);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/players")
    public Result<Boolean> addPlayer(@RequestBody Player player, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            player.setClubId(club.getId());
            playerMapper.insert(player);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/players/{id}")
    public Result<Boolean> updatePlayer(@PathVariable Long id, @RequestBody Player player) {
        try {
            player.setId(id);
            playerMapper.updateById(player);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/players/{id}")
    public Result<Boolean> deletePlayer(@PathVariable Long id) {
        try {
            playerMapper.deleteById(id);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 球员加入申请
    @GetMapping("/applications")
    public Result<List<PlayerApplication>> getApplications(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            List<PlayerApplication> applications = playerApplicationMapper.selectByClubId(club.getId());
            return Result.success(applications);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/applications/{id}/approve")
    public Result<Boolean> approveApplication(@PathVariable Long id) {
        try {
            PlayerApplication application = playerApplicationMapper.selectById(id);
            if (application == null) {
                return Result.error("申请不存在");
            }
            application.setStatus("APPROVED");
            playerApplicationMapper.updateById(application);

            // 更新球员的俱乐部ID
            Player player = playerMapper.selectById(application.getPlayerId());
            if (player != null) {
                player.setClubId(application.getClubId());
                playerMapper.updateById(player);
            }

            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/applications/{id}/reject")
    public Result<Boolean> rejectApplication(@PathVariable Long id) {
        try {
            PlayerApplication application = playerApplicationMapper.selectById(id);
            if (application == null) {
                return Result.error("申请不存在");
            }
            application.setStatus("REJECTED");
            playerApplicationMapper.updateById(application);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/player-apply/list")
    public Result<List<PlayerInfo>> getPlayerApplyList(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long userId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }
            List<PlayerInfo> list = playerInfoMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PlayerInfo>()
                            .eq("apply_team_id", club.getId())
                            .eq("join_status", "待审核")
                            .orderByDesc("id")
            );
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/player-apply/audit/{playerUserId}")
    public Result<Boolean> auditPlayerApply(
            @PathVariable Long playerUserId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long managerId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            TeamClub club = getManagerClub(managerId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            PlayerInfo playerInfo = playerInfoMapper.selectByUserId(playerUserId);
            if (playerInfo == null) {
                return Result.error("球员不存在");
            }
            if (playerInfo.getApplyTeamId() == null || !playerInfo.getApplyTeamId().equals(club.getId())) {
                return Result.error("该申请不属于你的俱乐部");
            }

            String action = String.valueOf(body.getOrDefault("action", ""));
            String remark = String.valueOf(body.getOrDefault("remark", ""));
            if ("APPROVE".equalsIgnoreCase(action)) {
                playerInfo.setJoinStatus("已审核");
                playerInfo.setStatus("俱乐部成员");
                playerInfo.setTeamId(club.getId());
                playerInfo.setIsFreeAgent(0);
            } else if ("REJECT".equalsIgnoreCase(action)) {
                playerInfo.setJoinStatus("拒绝");
                playerInfo.setStatus("自由身");
                playerInfo.setTeamId(null);
                playerInfo.setIsFreeAgent(1);
            } else {
                return Result.error("action必须为APPROVE或REJECT");
            }
            playerInfo.setClubRemark(remark);
            playerInfoMapper.updateById(playerInfo);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 球队赛程
    @GetMapping("/schedule")
    public Result<List<FootballMatch>> getSchedule(HttpServletRequest request, @RequestParam(required = false) String season) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            TeamClub club = getManagerClub(userId);
            if (club == null) {
                return Result.error("未找到你的俱乐部信息");
            }

            List<FootballMatch> matches = footballMatchMapper.selectByClubId(club.getId(), season);
            // 兜底：若 title/homename/awayname 未设置，则根据 home_team_id/away_team_id 补全，保证前端渲染“主队VS客队”
            if (matches != null) {
                for (FootballMatch m : matches) {
                    if (m == null) continue;
                    Long homeId = m.getHomeTeamId();
                    Long awayId = m.getAwayTeamId();
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
                    if (homeName != null) m.setHomeTeamName(homeName);
                    if (awayName != null) m.setAwayTeamName(awayName);
                    if (homeName != null && awayName != null) {
                        m.setTitle(homeName + " VS " + awayName);
                    }
                }
            }

            return Result.success(matches);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 联赛排行榜
    @GetMapping("/rankings")
    public Result<List<TeamClub>> getRankings() {
        try {
            List<TeamClub> clubs = teamClubMapper.selectRankedClubs();
            return Result.success(clubs);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 俱乐部端：获取“我的俱乐部”参加的联赛列表（用于积分榜切换）
     */
    @GetMapping("/my/leagues")
    public Result<List<League>> getMyLeagues(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long managerId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            TeamClub club = getManagerClub(managerId);
            if (club == null) return Result.error("未找到你的俱乐部信息");

            List<ClubLeagueRelation> rels = clubLeagueRelationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ClubLeagueRelation>()
                            .eq("club_id", club.getId())
                            .orderByAsc("league_id")
            );
            if (rels == null || rels.isEmpty()) return Result.success(new ArrayList<>());

            List<League> leagues = new ArrayList<>();
            for (ClubLeagueRelation r : rels) {
                if (r == null || r.getLeagueId() == null) continue;
                League l = leagueMapper.selectById(r.getLeagueId());
                if (l != null) leagues.add(l);
            }
            return Result.success(leagues);
        } catch (Exception e) {
            return Result.error("获取联赛列表失败：" + e.getMessage());
        }
    }
}
