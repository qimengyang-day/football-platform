package com.football.controller.player;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.football.common.Result;
import com.football.entity.PlayerInfo;
import com.football.entity.PlayerProfileUpdate;
import com.football.entity.TeamClub;
import com.football.mapper.PlayerInfoMapper;
import com.football.mapper.PlayerProfileUpdateMapper;
import com.football.mapper.TeamClubMapper;
import com.football.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/player")
public class PlayerController {
    @Autowired
    private PlayerInfoMapper playerInfoMapper;
    @Autowired
    private TeamClubMapper teamClubMapper;
    @Autowired
    private PlayerProfileUpdateMapper playerProfileUpdateMapper;

    /**
     * 获取当前登录用户的球员档案（从 JWT 解析 userId -> player_info.user_id）。
     */
    @GetMapping("/me")
    public Result<PlayerInfo> getMyProfile(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = auth.replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(token);
        PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
        // 兜底：历史数据可能缺失 player_info，自动补建默认档案
        if (playerInfo == null) {
            playerInfo = new PlayerInfo();
            playerInfo.setUserId(userId);
            playerInfo.setStatus("自由身");
            playerInfo.setIsFreeAgent(1);
            playerInfo.setGoals(0);
            playerInfo.setAssists(0);
            playerInfoMapper.insert(playerInfo);
        }
        return Result.success(playerInfo);
    }

    @GetMapping("/{id}")
    public Result<PlayerInfo> getPlayerProfile(@PathVariable Long id) {
        PlayerInfo playerInfo = playerInfoMapper.selectById(id);
        // 兼容：部分前端可能传的是 userId（player_info.user_id），而不是 player_info.id
        if (playerInfo == null) {
            playerInfo = playerInfoMapper.selectByUserId(id);
        }
        return Result.success(playerInfo);
    }

    @GetMapping("/stats/{id}")
    public Result<Object> getPlayerStats(@PathVariable Long id) {
        PlayerInfo playerInfo = playerInfoMapper.selectById(id);
        if (playerInfo == null) {
            playerInfo = playerInfoMapper.selectByUserId(id);
        }
        if (playerInfo != null) {
            return Result.success(playerInfo);
        }
        return Result.error("球员不存在");
    }

    @PutMapping("/profile")
    public Result<Boolean> updateProfile(@RequestBody PlayerInfo playerInfo, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo existing = playerInfoMapper.selectByUserId(userId);
            if (existing == null) {
                return Result.error("未找到你的球员档案");
            }

            // 只更新可编辑字段，避免前端传 null 把审核信息清空
            existing.setRealName(playerInfo.getRealName());
            existing.setHeight(playerInfo.getHeight());
            existing.setWeight(playerInfo.getWeight());
            existing.setPosition(playerInfo.getPosition());
            existing.setStatus(playerInfo.getStatus());
            existing.setTeamId(playerInfo.getTeamId());

            existing.setGoals(playerInfo.getGoals());
            existing.setAssists(playerInfo.getAssists());
            existing.setMarketValue(playerInfo.getMarketValue());
            existing.setIsFreeAgent("自由身".equals(playerInfo.getStatus()) ? 1 : 0);

            existing.setApplyTeamId(playerInfo.getApplyTeamId());
            existing.setApplyReason(playerInfo.getApplyReason());

            int result = playerInfoMapper.updateById(existing);
            if (result > 0) {
                return Result.success(true);
            }
            return Result.error("更新失败，未找到对应的球员信息");
        } catch (Exception e) {
            System.err.println("Update profile error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 球员档案修改提交管理员审核（不直接落 player_info）
     */
    @PostMapping("/profile/submit")
    public Result<Boolean> submitProfileUpdate(@RequestBody PlayerInfo payload, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long userId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            PlayerInfo existing = playerInfoMapper.selectByUserId(userId);
            if (existing == null) {
                existing = new PlayerInfo();
                existing.setUserId(userId);
                existing.setStatus("自由身");
                existing.setIsFreeAgent(1);
                existing.setGoals(0);
                existing.setAssists(0);
                playerInfoMapper.insert(existing);
            }

            // 若已有待审申请，则不允许重复提交
            PlayerProfileUpdate pending = playerProfileUpdateMapper.selectOne(
                    new QueryWrapper<PlayerProfileUpdate>()
                            .eq("player_user_id", userId)
                            .eq("status", "PENDING")
                            .last("limit 1")
            );
            if (pending != null) {
                return Result.error("你已有待审核的档案修改申请");
            }

            PlayerProfileUpdate req = new PlayerProfileUpdate();
            req.setPlayerUserId(userId);
            req.setRealName(payload.getRealName());
            req.setHeight(payload.getHeight());
            req.setWeight(payload.getWeight());
            req.setPosition(payload.getPosition());
            req.setNationality(payload.getNationality());
            req.setAge(payload.getAge());
            if (payload.getMarketValue() != null) {
                req.setMarketValue(java.math.BigDecimal.valueOf(payload.getMarketValue()));
            }
            // 注意：phone字段不在审核流程中，直接更新到player_info
            if (payload.getPhone() != null) {
                existing.setPhone(payload.getPhone());
                playerInfoMapper.updateById(existing);
            }
            req.setStatus("PENDING");
            req.setCreateTime(new java.util.Date());
            playerProfileUpdateMapper.insert(req);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("提交失败：" + e.getMessage());
        }
    }

    @GetMapping("/profile/submit/status")
    public Result<Object> getProfileUpdateStatus(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        Long userId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
        PlayerProfileUpdate latest = playerProfileUpdateMapper.selectOne(
                new QueryWrapper<PlayerProfileUpdate>()
                        .eq("player_user_id", userId)
                        .orderByDesc("id")
                        .last("limit 1")
        );
        return Result.success(latest);
    }

    @PostMapping("/apply-club")
    public Result<Boolean> applyClub(@RequestBody PlayerInfo playerInfo, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo existing = playerInfoMapper.selectByUserId(userId);
            if (existing == null) {
                return Result.error("未找到你的球员档案");
            }

            existing.setJoinStatus("待审核");
            existing.setApplyTeamId(playerInfo.getApplyTeamId());
            existing.setApplyReason(playerInfo.getApplyReason());

            int result = playerInfoMapper.updateById(existing);
            if (result > 0) {
                return Result.success(true);
            }
            return Result.error("申请失败，未找到对应的球员信息");
        } catch (Exception e) {
            System.err.println("Apply club error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("申请失败：" + e.getMessage());
        }
    }

    @GetMapping("/pending-applications")
    public Result<Object> getPendingApplications() {
        // 查询所有待审核的球员申请
        List<PlayerInfo> applications = playerInfoMapper.selectList(
            new QueryWrapper<PlayerInfo>().eq("join_status", "待审核")
        );
        return Result.success(applications);
    }

    // 新增：个人资料接口
    @GetMapping("/profile/get")
    public Result<PlayerInfo> getProfile(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = auth.replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(token);
        PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
        return Result.success(playerInfo);
    }

    // 新增：比赛记录接口
    @GetMapping("/match/record")
    public Result<Object> getMatchRecord(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = auth.replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(token);
        PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
        if (playerInfo != null) {
            return Result.success(playerInfo.getMatchRecord());
        }
        return Result.error("球员不存在");
    }

    // 新增：转会记录接口
    @GetMapping("/transfer/record")
    public Result<Object> getTransferRecord(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = auth.replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(token);
        PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
        if (playerInfo != null) {
            return Result.success(playerInfo.getTransferRecord());
        }
        return Result.error("球员不存在");
    }

    // 新增：俱乐部申请接口


    // 新增：更新个人资料（包含身价等字段）
    @PutMapping("/profile/update")
    public Result<Boolean> updateProfileInfo(@RequestBody PlayerInfo playerInfo, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo existing = playerInfoMapper.selectByUserId(userId);
            if (existing == null) {
                return Result.error("未找到你的球员档案");
            }

            // 更新字段
            if (playerInfo.getRealName() != null) existing.setRealName(playerInfo.getRealName());
            if (playerInfo.getHeight() != null) existing.setHeight(playerInfo.getHeight());
            if (playerInfo.getWeight() != null) existing.setWeight(playerInfo.getWeight());
            if (playerInfo.getPosition() != null) existing.setPosition(playerInfo.getPosition());
            if (playerInfo.getMarketValue() != null) existing.setMarketValue(playerInfo.getMarketValue());
            if (playerInfo.getIsFreeAgent() != null) existing.setIsFreeAgent(playerInfo.getIsFreeAgent());
            if (playerInfo.getTransferRecord() != null) existing.setTransferRecord(playerInfo.getTransferRecord());
            if (playerInfo.getMatchRecord() != null) existing.setMatchRecord(playerInfo.getMatchRecord());
            if (playerInfo.getStatus() != null) existing.setStatus(playerInfo.getStatus());
            if (playerInfo.getApplyTeamId() != null) existing.setApplyTeamId(playerInfo.getApplyTeamId());
            if (playerInfo.getApplyReason() != null) existing.setApplyReason(playerInfo.getApplyReason());
            if (playerInfo.getNationality() != null) existing.setNationality(playerInfo.getNationality());
            if (playerInfo.getAge() != null) existing.setAge(playerInfo.getAge());
            if (playerInfo.getPhone() != null) existing.setPhone(playerInfo.getPhone());

            int result = playerInfoMapper.updateById(existing);
            if (result > 0) {
                return Result.success(true);
            }
            return Result.error("更新失败，未找到对应的球员信息");
        } catch (Exception e) {
            System.err.println("Update profile error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    // 新增：获取球迷评价统计
    @GetMapping("/review/stats")
    public Result<Object> getReviewStats(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
            if (playerInfo == null) {
                return Result.error("未找到你的球员档案");
            }

            // 这里简化处理，实际应该从数据库中查询球迷评价数据
            return Result.success(new java.util.HashMap<String, Object>() {
                {
                    put("reviewCount", 123);
                    put("reviewScore", 4.5);
                }
            });
        } catch (Exception e) {
            System.err.println("Get review stats error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取评价统计失败：" + e.getMessage());
        }
    }

    // 新增：获取球迷评价列表
    @GetMapping("/review/list")
    public Result<Object> getReviewList(
            @RequestParam(required = false) Integer score,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            HttpServletRequest request
    ) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
            if (playerInfo == null) {
                return Result.error("未找到你的球员档案");
            }

            // 这里简化处理，实际应该从数据库中查询球迷评价数据
            java.util.List<java.util.Map<String, Object>> reviews = new java.util.ArrayList<>();
            for (int i = 0; i < 5; i++) {
                int idx = i + 1;
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("id", idx);
                row.put("username", "球迷" + idx);
                row.put("nickname", "球迷" + idx);
                row.put("avatar", null);
                row.put("score", 5 - i % 2);
                row.put("content", "球员表现非常出色，希望继续保持！" + idx);
                row.put("matchTitle", "英超联赛第" + idx + "轮");
                row.put("createTime", new java.util.Date());
                reviews.add(row);
            }

            return Result.success(new java.util.HashMap<String, Object>() {
                {
                    put("list", reviews);
                    put("total", reviews.size());
                }
            });
        } catch (Exception e) {
            System.err.println("Get review list error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取评价列表失败：" + e.getMessage());
        }
    }

    // 新增：获取当前俱乐部
    @GetMapping("/club/current")
    public Result<Object> getCurrentClub(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
            if (playerInfo == null) {
                return Result.error("未找到你的球员档案");
            }

            // 使用 teamId（实际所属俱乐部）而不是 applyTeamId（申请的俱乐部）
            if (playerInfo.getTeamId() != null) {
                TeamClub club = teamClubMapper.selectById(playerInfo.getTeamId());
                return Result.success(new java.util.HashMap<String, Object>() {
                    {
                        put("id", playerInfo.getTeamId());
                        put("name", club != null ? club.getName() : "未知俱乐部");
                        put("headCoach", club != null ? club.getHeadCoach() : null);
                        put("translator", club != null ? club.getTranslator() : null);
                        put("sponsor", club != null ? club.getSponsor() : null);
                    }
                });
            }
            return Result.success(null);
        } catch (Exception e) {
            System.err.println("Get current club error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取当前俱乐部失败：" + e.getMessage());
        }
    }

    /**
     * 球员端-我的俱乐部成员列表 + 总身价
     * 返回：{club, coach, totalValue, groups:{FW:[],MF:[],DF:[],GK:[],OTHER:[]}}
     */
    @GetMapping("/club/members")
    public Result<Object> getClubMembers(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long userId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            PlayerInfo me = playerInfoMapper.selectByUserId(userId);
            if (me == null) return Result.error("未找到你的球员档案");
            if (me.getTeamId() == null) return Result.success(null);

            TeamClub club = teamClubMapper.selectById(me.getTeamId());
            if (club == null) return Result.success(null);

            List<PlayerInfo> members = playerInfoMapper.selectList(
                    new QueryWrapper<PlayerInfo>().eq("team_id", club.getId())
            );

            java.util.Map<String, java.util.List<java.util.Map<String, Object>>> groups = new java.util.HashMap<>();
            groups.put("FW", new java.util.ArrayList<>());
            groups.put("MF", new java.util.ArrayList<>());
            groups.put("DF", new java.util.ArrayList<>());
            groups.put("GK", new java.util.ArrayList<>());
            groups.put("OTHER", new java.util.ArrayList<>());

            java.math.BigDecimal total = club.getCoachValue() == null ? java.math.BigDecimal.ZERO : club.getCoachValue();
            for (PlayerInfo pi : members) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("userId", pi.getUserId());
                row.put("name", pi.getRealName());
                row.put("age", pi.getAge());
                row.put("nationality", pi.getNationality());
                row.put("position", pi.getPosition());
                row.put("goals", pi.getGoals());
                row.put("assists", pi.getAssists());
                row.put("marketValue", pi.getMarketValue());

                if (pi.getMarketValue() != null) {
                    total = total.add(java.math.BigDecimal.valueOf(pi.getMarketValue()));
                }

                String key = "OTHER";
                String pos = pi.getPosition() == null ? "" : pi.getPosition();
                if (pos.contains("前锋")) key = "FW";
                else if (pos.contains("中场")) key = "MF";
                else if (pos.contains("后卫")) key = "DF";
                else if (pos.contains("门将")) key = "GK";
                groups.get(key).add(row);
            }

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("club", club);
            java.util.Map<String, Object> coach = new java.util.HashMap<>();
            coach.put("name", club.getHeadCoach());
            coach.put("value", club.getCoachValue());
            data.put("coach", coach);
            data.put("totalValue", total);
            data.put("groups", groups);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取俱乐部成员失败：" + e.getMessage());
        }
    }

    // 新增：获取申请状态
    @GetMapping("/club/application/status")
    public Result<Object> getApplicationStatus(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
            if (playerInfo == null) {
                return Result.error("未找到你的球员档案");
            }

            // 这里简化处理，实际应该从数据库中查询申请状态
            if (playerInfo.getJoinStatus() != null && !playerInfo.getJoinStatus().equals("")) {
                TeamClub club = playerInfo.getApplyTeamId() == null ? null : teamClubMapper.selectById(playerInfo.getApplyTeamId());
                return Result.success(new java.util.HashMap<String, Object>() {
                    {
                        put("status", playerInfo.getJoinStatus());
                        put("clubName", club != null ? club.getName() : null);
                        put("applyTime", new java.util.Date());
                        put("auditTime", new java.util.Date());
                        put("auditRemark", playerInfo.getClubRemark());
                    }
                });
            }
            return Result.success(null);
        } catch (Exception e) {
            System.err.println("Get application status error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("获取申请状态失败：" + e.getMessage());
        }
    }

    // 新增：离开俱乐部（提交申请，需要管理员审核）
    @PostMapping("/club/leave")
    public Result<Boolean> leaveClub(@RequestBody java.util.Map<String, Object> params, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
            if (playerInfo == null) {
                return Result.error("未找到你的球员档案");
            }

            // 检查是否有俱乐部
            if (playerInfo.getTeamId() == null) {
                return Result.error("您目前没有所属俱乐部");
            }

            String reason = params.get("reason") != null ? params.get("reason").toString() : "";

            // 设置离队申请状态
            playerInfo.setJoinStatus("离队待审核");
            playerInfo.setApplyReason(reason); // 复用applyReason存储离队原因

            int result = playerInfoMapper.updateById(playerInfo);
            if (result > 0) {
                return Result.success(true);
            }
            return Result.error("提交离队申请失败");
        } catch (Exception e) {
            System.err.println("Leave club apply error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("提交离队申请失败：" + e.getMessage());
        }
    }

    // 新增：申请加入俱乐部
    @PostMapping("/club/apply")
    public Result<Boolean> applyClub(@RequestBody java.util.Map<String, Object> params, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            PlayerInfo playerInfo = playerInfoMapper.selectByUserId(userId);
            if (playerInfo == null) {
                return Result.error("未找到你的球员档案");
            }

            Long clubId = Long.valueOf(params.get("clubId").toString());
            String reason = params.get("reason").toString();

            // 更新申请信息
            playerInfo.setJoinStatus("待审核");
            playerInfo.setApplyTeamId(clubId);
            playerInfo.setApplyReason(reason);

            int result = playerInfoMapper.updateById(playerInfo);
            if (result > 0) {
                return Result.success(true);
            }
            return Result.error("申请失败，未找到对应的球员信息");
        } catch (Exception e) {
            System.err.println("Apply club error: " + e.getMessage());
            e.printStackTrace();
            return Result.error("申请失败：" + e.getMessage());
        }
    }
}

