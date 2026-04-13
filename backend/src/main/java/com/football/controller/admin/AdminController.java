package com.football.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.common.Result;
import com.football.entity.*;
import com.football.mapper.*;
import com.football.service.ClubService;
import com.football.service.DashboardService;
import com.football.service.FanService;
import com.football.service.LeagueService;
import com.football.service.PlayerService;
import com.football.utils.JwtUtil;
import com.football.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private PlayerService playerService;
    @Autowired
    private FanService fanService;
    @Autowired
    private ClubService clubService;
    @Autowired
    private LeagueService leagueService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private LeagueMapper leagueMapper;
    @Autowired
    private TeamClubMapper teamClubMapper;
    @Autowired
    private ClubLeagueRelationMapper clubLeagueRelationMapper;
    @Autowired
    private PlayerProfileUpdateMapper playerProfileUpdateMapper;
    @Autowired
    private PlayerInfoMapper playerInfoMapper;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private ClubSponsorMapper clubSponsorMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtil redisUtil;

    // 球员管理
    @GetMapping("/player/list")
    public Result<Page<PlayerInfo>> getPlayerList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PlayerInfo> page = playerService.getPlayerList(pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/players")
    public Result<Object> getPlayers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String clubId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            // 先从 Redis 缓存获取
            String searchKey = (search != null && !search.trim().isEmpty()) ? search.trim() : "";
            String clubIdKey = (clubId != null && !clubId.trim().isEmpty()) ? clubId.trim() : "0";
            String cacheKey = "admin:players:" + searchKey + ":" + clubIdKey + ":" + pageNum + ":" + pageSize;
            try {
                Object cached = redisUtil.get(cacheKey);
                if (cached != null) {
                    System.out.println("从缓存获取球员列表: search='" + searchKey + "', clubId=" + clubIdKey + ", page=" + pageNum);
                    return Result.success(cached);
                }
            } catch (Exception e) {
                System.out.println("Redis 缓存读取失败: " + e.getMessage());
            }
            
            Long clubIdVal = null;
            if (clubId != null && !clubId.trim().isEmpty() && !"0".equals(clubId.trim())) {
                clubIdVal = Long.parseLong(clubId.trim());
            }
            Map<String, Object> result = playerService.getPlayers(search, clubIdVal, pageNum, pageSize);
            
            // 存入 Redis 缓存 (10分钟过期)
            try {
                redisUtil.set(cacheKey, result, 600);
                System.out.println("管理员球员列表已缓存: search='" + searchKey + "', clubId=" + clubIdKey + ", page=" + pageNum);
            } catch (Exception e) {
                System.out.println("Redis 缓存存储失败: " + e.getMessage());
            }
            
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取球员列表失败", e);
            return Result.error("获取球员列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/player/add")
    public Result<Boolean> addPlayer(@RequestBody PlayerInfo playerInfo) {
        boolean success = playerService.addPlayer(playerInfo);
        if (success) {
            // 清除球员列表缓存和仪表盘缓存
            try {
                redisUtil.deleteByPattern("fan:players:*");
                redisUtil.deleteByPattern("admin:players:*");
                redisUtil.delete("admin:dashboard:stats");
                if (playerInfo.getTeamId() != null) {
                    redisUtil.delete("club:personnel:" + playerInfo.getTeamId());
                    redisUtil.delete("admin:club:summary:" + playerInfo.getTeamId());
                }
                System.out.println("新增球员成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    @PutMapping("/player/update")
    public Result<Boolean> updatePlayer(@RequestBody PlayerInfo playerInfo) {
        boolean success = playerService.updatePlayer(playerInfo);
        if (success) {
            // 清除球员列表缓存，并清除旧俱乐部和新俱乐部的人员缓存
            try {
                redisUtil.deleteByPattern("fan:players:*");
                redisUtil.deleteByPattern("admin:players:*");
                if (playerInfo.getTeamId() != null) {
                    redisUtil.delete("club:personnel:" + playerInfo.getTeamId());
                    redisUtil.delete("admin:club:summary:" + playerInfo.getTeamId());
                }
                // 注意：如果球员转会，还需要清除旧俱乐部的缓存（这里暂不处理，因未获取旧 clubId）
                System.out.println("更新球员成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    @DeleteMapping("/player/delete/{id}")
    public Result<Boolean> deletePlayer(@PathVariable Long id) {
        boolean success = playerService.deletePlayer(id);
        if (success) {
            // 清除球员列表缓存和仪表盘缓存
            try {
                redisUtil.deleteByPattern("fan:players:*");
                redisUtil.deleteByPattern("admin:players:*");
                redisUtil.delete("admin:dashboard:stats");
                System.out.println("删除球员成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    // 球迷管理
    @GetMapping("/fan/list")
    public Result<Page<SysUser>> getFanList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<SysUser> page = fanService.getFanList(pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/fans")
    public Result<Object> getFans(
            @RequestParam(required = false) String search,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        Map<String, Object> result = fanService.getFans(search, pageNum, pageSize);
        return Result.success(result);
    }

    @PutMapping("/fan/status")
    public Result<Boolean> updateFanStatus(@RequestParam Long userId, @RequestParam Integer status) {
        boolean success = fanService.updateFanStatus(userId, status);
        return Result.success(success);
    }

    @PutMapping("/fan/status/{id}")
    public Result<Boolean> updateFanStatus(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        int status = Integer.parseInt(params.get("status").toString());
        boolean success = fanService.updateFanStatus(id, status);
        return Result.success(success);
    }

    @GetMapping("/fan/detail/{id}")
    public Result<SysUser> getFanDetail(@PathVariable Long id) {
        SysUser user = fanService.getFanDetail(id);
        return Result.success(user);
    }

    // 俱乐部管理
    @PostMapping("/club/add")
    public Result<Long> addClub(@RequestBody TeamClub club, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录或登录状态已失效");
            }
            if (club == null || club.getName() == null || club.getName().trim().isEmpty()) {
                return Result.error("俱乐部名称不能为空");
            }
            String token = auth.replace("Bearer ", "");
            Long adminId = JwtUtil.getUserIdFromToken(token);
            boolean success = clubService.addClub(club, adminId);
            if (success && club.getId() != null) {
                // 清除俱乐部列表缓存和仪表盘缓存
                try {
                    redisUtil.delete("club:list:all");
                    redisUtil.delete("admin:dashboard:stats");
                    System.out.println("新增俱乐部成功，已清除相关缓存");
                } catch (Exception e) {
                    System.out.println("清除缓存失败: " + e.getMessage());
                }
                return Result.success(club.getId());
            }
            return Result.error("添加俱乐部失败：请检查俱乐部名称是否重复或俱乐部管理员绑定是否冲突");
        } catch (Exception e) {
            return Result.error("添加俱乐部异常：" + e.getMessage());
        }
    }

    @GetMapping("/club/list")
    public Result<Page<TeamClub>> getClubList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<TeamClub> page = clubService.getClubList(pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/clubs")
    public Result<Object> getClubs(
            @RequestParam(required = false) String search,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        Map<String, Object> result = clubService.getClubs(search, pageNum, pageSize);
        return Result.success(result);
    }

    @PutMapping("/club/update")
    public Result<Boolean> updateClub(@RequestBody TeamClub club) {
        boolean success = clubService.updateClub(club);
        if (success) {
            // 清除俱乐部相关缓存
            try {
                redisUtil.delete("club:list:all");
                if (club.getId() != null) {
                    redisUtil.delete("club:personnel:" + club.getId());
                    redisUtil.delete("admin:club:summary:" + club.getId());
                }
                System.out.println("更新俱乐部成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    @DeleteMapping("/club/delete/{id}")
    public Result<Boolean> deleteClub(@PathVariable Long id) {
        boolean success = clubService.deleteClub(id);
        if (success) {
            // 清除俱乐部相关缓存
            try {
                redisUtil.delete("club:list:all");
                redisUtil.delete("club:personnel:" + id);
                redisUtil.delete("admin:club:summary:" + id);
                redisUtil.delete("admin:dashboard:stats");
                redisUtil.deleteByPattern("league:clubs:*");
                System.out.println("删除俱乐部成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    // 联赛管理
    @PostMapping("/league/add")
    public Result<Boolean> addLeague(@RequestBody League league) {
        boolean success = leagueService.addLeague(league);
        if (success) {
            // 清除联赛列表缓存
            try {
                redisUtil.delete("league:list:all");
                redisUtil.delete("admin:dashboard:stats");
                System.out.println("新增联赛成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    @GetMapping("/league/list")
    public Result<Page<League>> getLeagueList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<League> page = leagueService.getLeagueList(pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/leagues")
    public Result<Object> getLeagues(
            @RequestParam(required = false) String search,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        Map<String, Object> result = leagueService.getLeagues(search, pageNum, pageSize);
        return Result.success(result);
    }

    @PutMapping("/league/update")
    public Result<Boolean> updateLeague(@RequestBody League league) {
        boolean success = leagueService.updateLeague(league);
        if (success) {
            // 清除联赛列表缓存
            try {
                redisUtil.delete("league:list:all");
                redisUtil.delete("admin:dashboard:stats");
                System.out.println("更新联赛成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    @DeleteMapping("/league/delete/{id}")
    public Result<Boolean> deleteLeague(@PathVariable Long id) {
        boolean success = leagueService.deleteLeague(id);
        if (success) {
            // 清除联赛列表缓存
            try {
                redisUtil.delete("league:list:all");
                redisUtil.delete("admin:dashboard:stats");
                System.out.println("删除联赛成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    @PostMapping("/league/club/add")
    public Result<Boolean> addClubToLeague(@RequestParam Long clubId, @RequestParam Long leagueId) {
        boolean success = leagueService.addClubToLeague(clubId, leagueId);
        if (success) {
            // 清除联赛俱乐部列表缓存
            try {
                redisUtil.delete("league:clubs:" + leagueId);
                System.out.println("俱乐部加入联赛成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
        }
        return Result.success(success);
    }

    @DeleteMapping("/club/leagues/{clubId}")
    public Result<Boolean> deleteClubLeagues(@PathVariable Long clubId) {
        try {
            // 删除俱乐部与所有联赛的关联
            clubLeagueRelationMapper.delete(
                new QueryWrapper<ClubLeagueRelation>().eq("club_id", clubId)
            );
            
            // 清除所有联赛的俱乐部列表缓存
            try {
                redisUtil.deleteByPattern("league:clubs:*");
                System.out.println("俱乐部退出联赛成功，已清除相关缓存");
            } catch (Exception e) {
                System.out.println("清除缓存失败: " + e.getMessage());
            }
            
            return Result.success(true);
        } catch (Exception e) {
            logger.error("删除俱乐部联赛关联失败:", e);
            return Result.error("删除俱乐部联赛关联失败：" + e.getMessage());
        }
    }

    @GetMapping("/league/clubs/{leagueId}")
    public Result<Object> getLeagueClubs(@PathVariable Long leagueId) {
        // 先从 Redis 缓存获取
        String cacheKey = "league:clubs:" + leagueId;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取联赛 " + leagueId + " 的俱乐部列表");
                return Result.success((Object) cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        try {
            // 查询联赛下的所有俱乐部
            List<ClubLeagueRelation> relations = clubLeagueRelationMapper.selectList(
                new QueryWrapper<ClubLeagueRelation>().eq("league_id", leagueId)
            );
            
            List<TeamClub> clubs = new ArrayList<>();
            for (ClubLeagueRelation relation : relations) {
                TeamClub club = teamClubMapper.selectById(relation.getClubId());
                if (club != null) {
                    clubs.add(club);
                }
            }
            
            // 存入 Redis 缓存 (30分钟过期)
            try {
                redisUtil.set(cacheKey, clubs, 1800);
                System.out.println("联赛 " + leagueId + " 俱乐部列表已缓存，共 " + clubs.size() + " 个俱乐部");
            } catch (Exception e) {
                System.out.println("Redis 缓存存储失败: " + e.getMessage());
            }
            
            return Result.success(clubs);
        } catch (Exception e) {
            logger.error("获取联赛俱乐部失败:", e);
            return Result.error("获取联赛俱乐部失败: " + e.getMessage());
        }
    }

    // 数据概览
    @GetMapping("/dashboard")
    public Result<Object> getDashboardData() {
        // 先从 Redis 缓存获取
        String cacheKey = "admin:dashboard:stats";
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取仪表盘统计数据");
                return Result.success(cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        Map<String, Object> data = dashboardService.getDashboardData();
        
        // 存入 Redis 缓存 (5分钟过期)
        try {
            redisUtil.set(cacheKey, data, 300);
            System.out.println("仪表盘统计数据已缓存");
        } catch (Exception e) {
            System.out.println("Redis 缓存存储失败: " + e.getMessage());
        }
        
        return Result.success(data);
    }

    /**
     * 球员档案修改申请（管理员审核）
     */
    @GetMapping("/player/profile-updates")
    public Result<Object> getPlayerProfileUpdates(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status
    ) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PlayerProfileUpdate> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        QueryWrapper<PlayerProfileUpdate> qw = new QueryWrapper<>();
        if (status != null && !status.trim().isEmpty()) {
            qw.eq("status", status.trim());
        }
        qw.orderByDesc("id");
        playerProfileUpdateMapper.selectPage(page, qw);
        long total = playerProfileUpdateMapper.selectCount(qw);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", total);
        return Result.success(data);
    }

    @PutMapping("/player/profile-updates/{id}/audit")
    public Result<Boolean> auditPlayerProfileUpdate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String action = String.valueOf(body.getOrDefault("action", ""));
            String remark = String.valueOf(body.getOrDefault("remark", ""));

            PlayerProfileUpdate req = playerProfileUpdateMapper.selectById(id);
            if (req == null) return Result.error("申请不存在");
            if (!"PENDING".equals(req.getStatus())) return Result.error("该申请已处理");

            if ("APPROVE".equalsIgnoreCase(action)) {
                // 应用到 player_info
                PlayerInfo pi = playerInfoMapper.selectByUserId(req.getPlayerUserId());
                if (pi == null) return Result.error("球员不存在");
                pi.setRealName(req.getRealName());
                pi.setHeight(req.getHeight());
                pi.setWeight(req.getWeight());
                pi.setPosition(req.getPosition());
                pi.setNationality(req.getNationality());
                pi.setAge(req.getAge());
                if (req.getMarketValue() != null) {
                    pi.setMarketValue(req.getMarketValue().doubleValue());
                }
                // 将本次审核备注同步到正式档案（便于球员端展示“已通过/备注”）
                pi.setAdminRemark(remark);
                playerInfoMapper.updateById(pi);
                // 通过后：按需求清理临时表记录（正式表已落库）
                req.setStatus("APPROVED");
            } else if ("REJECT".equalsIgnoreCase(action)) {
                // 驳回后：按需求删除临时表这条记录（不落正式表）
                req.setStatus("REJECTED");
            } else {
                return Result.error("action必须为APPROVE或REJECT");
            }

            // 审核落库 & 清理临时记录
            req.setAdminRemark(remark);
            req.setAuditTime(new Date());
            // 更新审核状态到数据库（保留记录供查看）
            playerProfileUpdateMapper.updateById(req);
            // 不删除记录，保留在列表中供查看历史
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("审核失败：" + e.getMessage());
        }
    }


    // 用户管理
    @PostMapping("/user/add")
    public Result<Boolean> addUser(@RequestBody SysUser user) {
        try {
            // 俱乐部角色：强制绑定所属俱乐部（通过 mainTeamId）
            if ("CLUB".equals(user.getRole())) {
                if (user.getMainTeamId() == null) {
                    return Result.error("新增俱乐部角色时必须填写所属俱乐部（mainTeamId）");
                }
                TeamClub club = teamClubMapper.selectById(user.getMainTeamId());
                if (club == null) {
                    return Result.error("所选俱乐部不存在");
                }
                if (club.getManagerId() != null) {
                    return Result.error("该俱乐部已绑定其他俱乐部角色，无法再次绑定");
                }
            }

            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                // 若不是 bcrypt 密文，则按明文加密
                if (!user.getPassword().startsWith("$2")) {
                    user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
                }
            }
            int result = sysUserMapper.insert(user);
            if (result > 0 && "CLUB".equals(user.getRole()) && user.getMainTeamId() != null) {
                // 绑定 manager_id 到俱乐部
                TeamClub club = teamClubMapper.selectById(user.getMainTeamId());
                if (club != null) {
                    club.setManagerId(user.getId());
                    teamClubMapper.updateById(club);
                }
            }
            return Result.success(result > 0);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/user/update")
    public Result<Boolean> updateUser(@RequestBody SysUser user) {
        try {
            // 俱乐部角色：如果更新了所属俱乐部，需要迁移 manager_id
            if ("CLUB".equals(user.getRole())) {
                if (user.getMainTeamId() == null) {
                    return Result.error("更新俱乐部角色时必须填写所属俱乐部（mainTeamId）");
                }
                TeamClub newClub = teamClubMapper.selectById(user.getMainTeamId());
                if (newClub == null) {
                    return Result.error("所选俱乐部不存在");
                }
                if (newClub.getManagerId() != null && !newClub.getManagerId().equals(user.getId())) {
                    return Result.error("该俱乐部已绑定其他俱乐部角色，无法再次绑定");
                }
            }

            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                if (!user.getPassword().startsWith("$2")) {
                    user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
                }
            }
            int result = sysUserMapper.updateById(user);
            if (result > 0 && "CLUB".equals(user.getRole()) && user.getMainTeamId() != null) {
                // 重置所有 manager_id 归属后，确保只有当前用户的 mainTeamId 绑定到自己
                List<TeamClub> clubs = teamClubMapper.selectList(
                        new QueryWrapper<TeamClub>().eq("manager_id", user.getId())
                );
                if (clubs != null) {
                    for (TeamClub c : clubs) {
                        if (c != null && c.getId() != null && !c.getId().equals(user.getMainTeamId())) {
                            c.setManagerId(null);
                            teamClubMapper.updateById(c);
                        }
                    }
                }

                TeamClub club = teamClubMapper.selectById(user.getMainTeamId());
                if (club != null) {
                    club.setManagerId(user.getId());
                    teamClubMapper.updateById(club);
                }
            }
            return Result.success(result > 0);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/user/delete/{id}")
    public Result<Boolean> deleteUser(@PathVariable Long id) {
        try {
            SysUser user = sysUserMapper.selectById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 俱乐部角色删除：级联删除该俱乐部涉及的联赛关联信息，并清空俱乐部的 managerId
            if ("CLUB".equals(user.getRole())) {
                List<TeamClub> clubs = teamClubMapper.selectList(
                        new QueryWrapper<TeamClub>().eq("manager_id", id)
                );
                if (clubs != null && !clubs.isEmpty()) {
                    for (TeamClub club : clubs) {
                        if (club == null || club.getId() == null) continue;
                        // 删除 club_league_relation 关联
                        clubLeagueRelationMapper.delete(
                                new QueryWrapper<ClubLeagueRelation>().eq("club_id", club.getId())
                        );
                        // 清空俱乐部 manager_id，避免 club role 已删除导致的脏关联
                        club.setManagerId(null);
                        teamClubMapper.updateById(club);
                    }
                }
            }

            int result = sysUserMapper.deleteById(id);
            return Result.success(result > 0);
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    public Result<Object> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        // 俱乐部账号在「俱乐部信息」中管理，此处列表与分页条数一致，避免前端再过滤导致「每页条数无效」
        queryWrapper.ne("role", "CLUB");
        if (search != null && !search.equals("")) {
            queryWrapper.and(w -> w.like("username", search).or().like("nickname", search));
        }
        queryWrapper.orderByDesc("id");
        sysUserMapper.selectPage(page, queryWrapper);
        long total = sysUserMapper.selectCount(queryWrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", total);
        return Result.success(result);
    }

    // 管理员：查看某个俱乐部的人员状况（教练/翻译/赞助商/球员/总身价）
    @GetMapping("/club/{clubId}/summary")
    public Result<Object> getClubPersonnelSummary(@PathVariable Long clubId) {
        // 先从 Redis 缓存获取
        String cacheKey = "admin:club:summary:" + clubId;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取俱乐部 " + clubId + " 人员摘要");
                return Result.success((Object) cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        try {
            TeamClub club = teamClubMapper.selectById(clubId);
            if (club == null) {
                return Result.error("未找到俱乐部信息");
            }

            // coach 表：主教练/翻译（允许多个）
            List<Coach> coachList = coachMapper.selectList(
                    new QueryWrapper<Coach>().eq("club_id", clubId)
            );

            String headCoach = null;
            java.util.List<String> translatorNames = new java.util.ArrayList<>();
            java.math.BigDecimal coachValue = java.math.BigDecimal.ZERO;

            if (coachList != null) {
                for (Coach c : coachList) {
                    String position = c.getPosition();
                    if (position == null) continue;

                    if (position.contains("主教练")) {
                        if (headCoach == null) headCoach = c.getName();
                        if (c.getSalary() != null) coachValue = java.math.BigDecimal.valueOf(c.getSalary());
                    } else if (position.contains("翻译")) {
                        if (c.getName() != null && !c.getName().trim().isEmpty()) {
                            translatorNames.add(c.getName().trim());
                        }
                    }
                }
            }

            // sponsor：club_sponsor（多条）
            List<ClubSponsor> sponsors = clubSponsorMapper.selectList(
                    new QueryWrapper<ClubSponsor>().eq("club_id", clubId)
            );
            java.util.List<String> sponsorNames = new java.util.ArrayList<>();
            if (sponsors != null) {
                for (ClubSponsor s : sponsors) {
                    if (s.getSponsorName() != null && !s.getSponsorName().trim().isEmpty()) {
                        sponsorNames.add(s.getSponsorName().trim());
                    }
                }
            }

            // 球员：player_info.team_id
            List<PlayerInfo> members = playerInfoMapper.selectList(
                    new QueryWrapper<PlayerInfo>().eq("team_id", clubId)
            );
            java.math.BigDecimal totalValue = coachValue == null ? java.math.BigDecimal.ZERO : coachValue;
            if (members != null) {
                for (PlayerInfo pi : members) {
                    if (pi != null && pi.getMarketValue() != null) {
                        totalValue = totalValue.add(java.math.BigDecimal.valueOf(pi.getMarketValue()));
                    }
                }
            }

            // 仅返回前 20 名，避免弹窗过长
            java.util.List<java.util.Map<String, Object>> players = new java.util.ArrayList<>();
            if (members != null) {
                for (int i = 0; i < members.size() && i < 20; i++) {
                    PlayerInfo pi = members.get(i);
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    row.put("id", pi.getUserId());
                    row.put("realName", pi.getRealName());
                    row.put("position", pi.getPosition());
                    row.put("age", pi.getAge());
                    row.put("nationality", pi.getNationality());
                    row.put("marketValue", pi.getMarketValue());
                    players.add(row);
                }
            }

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("club", club);
            data.put("headCoach", headCoach != null ? headCoach : club.getHeadCoach());
            data.put("translators", java.lang.String.join(",", translatorNames));
            data.put("sponsors", sponsorNames);
            data.put("playerCount", members != null ? members.size() : 0);
            data.put("totalValue", totalValue.doubleValue());
            data.put("players", players);
            
            // 存入 Redis 缓存 (15分钟过期)
            try {
                redisUtil.set(cacheKey, data, 900);
                System.out.println("俱乐部 " + clubId + " 人员摘要已缓存");
            } catch (Exception e) {
                System.out.println("Redis 缓存存储失败: " + e.getMessage());
            }
            
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取俱乐部人员状况失败：" + e.getMessage());
        }
    }

    // 重置用户密码
    @PutMapping("/user/reset-password/{id}")
    public Result<Boolean> resetPassword(@PathVariable Long id) {
        try {
            SysUser user = sysUserMapper.selectById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            // 重置密码为123456（按规则进行加密）
            user.setPassword(passwordEncoder.encode("123456"));
            int result = sysUserMapper.updateById(user);
            return Result.success(result > 0);
        } catch (Exception e) {
            logger.error("重置密码失败:", e);
            return Result.error("重置密码失败: " + e.getMessage());
        }
    }
}
