package com.football.controller.fan;

import com.football.common.Result;
import com.football.dto.FanCommentCreateDTO;
import com.football.entity.FanComment;
import com.football.entity.FanCommentReply;
import com.football.entity.FanFollowClub;
import com.football.entity.FanTeamRelation;
import com.football.entity.FootballMatch;
import com.football.entity.MatchRating;
import com.football.entity.MatchScore;
import com.football.entity.TeamClub;
import com.football.mapper.FanFollowClubMapper;
import com.football.mapper.FanCommentMapper;
import com.football.mapper.FanCommentReplyMapper;
import com.football.mapper.FootballMatchMapper;
import com.football.mapper.MatchRatingMapper;
import com.football.mapper.SysUserMapper;
import com.football.mapper.TeamClubMapper;
import com.football.mapper.PlayerInfoMapper;
import com.football.utils.JwtUtil;
import com.football.utils.RedisUtil;
import com.football.vo.FanCommentReplyVO;
import com.football.vo.FanCommentVO;
import com.football.vo.FanPlayerListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/api/fan")
public class FanController {
    @Autowired
    private FanCommentMapper fanCommentMapper;

    @Autowired
    private FanCommentReplyMapper fanCommentReplyMapper;
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private FanFollowClubMapper fanFollowClubMapper;

    @Autowired
    private TeamClubMapper teamClubMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private FootballMatchMapper footballMatchMapper;

    @Autowired
    private MatchRatingMapper matchRatingMapper;

    @Autowired
    private PlayerInfoMapper playerInfoMapper;

    /**
     * 球迷端-球员列表（分页+搜索）
     * 返回结构：{records:[], total:n}
     */
    @GetMapping("/players")
    public Result<Object> getPlayers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String search
    ) {
        // 先从 Redis 缓存获取
        String searchKey = (search != null && !search.trim().isEmpty()) ? search.trim() : "";
        String cacheKey = "fan:players:" + searchKey + ":" + page + ":" + pageSize;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取球员列表: search='" + searchKey + "', page=" + page);
                return Result.success(cached);
            }
        } catch (Exception e) {
            System.out.println("Redis 缓存读取失败: " + e.getMessage());
        }
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.football.entity.PlayerInfo> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.football.entity.PlayerInfo> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();

        if (search != null && !search.trim().isEmpty()) {
            qw.like("real_name", search.trim()).or().like("position", search.trim());
        }
        qw.orderByDesc("id");

        playerInfoMapper.selectPage(p, qw);
        long total = playerInfoMapper.selectCount(qw);
        List<com.football.entity.PlayerInfo> rows = p.getRecords();

        List<FanPlayerListVO> records = new ArrayList<>();
        for (com.football.entity.PlayerInfo pi : rows) {
            FanPlayerListVO vo = new FanPlayerListVO();
            vo.setId(pi.getUserId());
            vo.setName(pi.getRealName());
            vo.setPosition(pi.getPosition());
            vo.setAge(pi.getAge());
            vo.setNationality(pi.getNationality());
            vo.setMarketValue(pi.getMarketValue());

            String clubName = "自由身";
            if (pi.getTeamId() != null) {
                TeamClub club = teamClubMapper.selectById(pi.getTeamId());
                if (club != null) clubName = club.getName();
            }
            vo.setClubName(clubName);
            records.add(vo);
        }

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("records", records);
        data.put("total", total);
        
        // 存入 Redis 缓存 (10分钟过期)
        try {
            redisUtil.set(cacheKey, data, 600);
            System.out.println("球员列表已缓存: search='" + searchKey + "', page=" + page + ", total=" + total);
        } catch (Exception e) {
            System.out.println("Redis 缓存存储失败: " + e.getMessage());
        }
        
        return Result.success(data);
    }

    @PostMapping("/comment")
    public Result<Boolean> addComment(@RequestBody FanComment comment) {
        fanCommentMapper.insert(comment);
        // 清除相关缓存
        try {
            if (comment.getMatchId() != null) {
                redisUtil.delete("fan:comments:match:" + comment.getMatchId());
            }
            redisUtil.deleteByPattern("fan:comments:hot*");
            System.out.println("评论已添加，相关缓存已清除");
        } catch (Exception e) {
            System.err.println("Redis 缓存清除失败，不影响业务: " + e.getMessage());
        }
        return Result.success(true);
    }

    /**
     * 热门评论（类懂球帝"热门评论"展示）
     */
    @GetMapping("/comment/hot")
    public Result<List<FanCommentVO>> getHotComments(
            @RequestParam(value = "limit", required = false, defaultValue = "8") Integer limit
    ) {
        // 先从 Redis 缓存获取
        String cacheKey = "fan:comments:hot:" + limit;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取热门评论: limit=" + limit);
                return Result.success((List<FanCommentVO>) cached);
            }
        } catch (Exception e) {
            System.err.println("Redis 缓存读取失败，降级到数据库查询: " + e.getMessage());
        }
    
        List<FanComment> comments = fanCommentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanComment>()
                        .orderByDesc("likes")
                        .last("limit " + limit)
        );
    
        // 简单 enrich：username/avatar/matchTitle
        List<FanCommentVO> vos = new ArrayList<>();
        for (FanComment c : comments) {
            FanCommentVO vo = new FanCommentVO();
            vo.setId(c.getId());
            vo.setMatchId(c.getMatchId());
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setLikes(c.getLikes());
            vo.setCreateTime(c.getCreateTime());
    
            // 由于matchTitle在多个comment里重复，简单做一次查询也够用
            FootballMatch match = footballMatchMapper.selectById(c.getMatchId());
            vo.setMatchTitle(match != null ? match.getTitle() : null);
    
            com.football.entity.SysUser user = sysUserMapper.selectById(c.getUserId());
            vo.setUsername(user != null ? user.getUsername() : null);
            vo.setAvatar(user != null ? user.getAvatar() : null);
    
            vo.setReplies(Collections.emptyList());
            vo.setReplyCount(0);
            vo.setMyReplyCount(0);
            vos.add(vo);
        }
    
        // 存入 Redis 缓存 (5分钟过期)
        try {
            redisUtil.set(cacheKey, vos, 300);
            System.out.println("热门评论已缓存: limit=" + limit + ", count=" + vos.size());
        } catch (Exception e) {
            System.err.println("Redis 缓存存储失败，不影响业务: " + e.getMessage());
        }
    
        return Result.success(vos);
    }

    /**
     * 获取某场比赛的评论列表
     */
    @GetMapping("/match/{matchId}/comments")
    public Result<List<FanCommentVO>> listMatchComments(@PathVariable Long matchId, HttpServletRequest request) {
        // 先从 Redis 缓存获取
        String cacheKey = "fan:comments:match:" + matchId;
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                System.out.println("从缓存获取比赛评论: matchId=" + matchId);
                return Result.success((List<FanCommentVO>) cached);
            }
        } catch (Exception e) {
            System.err.println("Redis 缓存读取失败，降级到数据库查询: " + e.getMessage());
        }

        List<FanComment> comments = fanCommentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanComment>()
                        .eq("match_id", matchId)
                        .orderByDesc("create_time")
        );

        FootballMatch match = footballMatchMapper.selectById(matchId);
        String matchTitle = match != null ? match.getTitle() : null;

        Long myUserId = null;
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            myUserId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
        }

        List<FanCommentVO> vos = new ArrayList<>();
        for (FanComment c : comments) {
            FanCommentVO vo = new FanCommentVO();
            vo.setId(c.getId());
            vo.setMatchId(c.getMatchId());
            vo.setMatchTitle(matchTitle);
            vo.setUserId(c.getUserId());
            vo.setContent(c.getContent());
            vo.setLikes(c.getLikes());
            vo.setCreateTime(c.getCreateTime());
            vo.setReplies(Collections.emptyList());
            Integer replyCount = fanCommentReplyMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanCommentReply>()
                            .eq("comment_id", c.getId())
            ).intValue();
            vo.setReplyCount(replyCount);

            if (myUserId != null) {
                Integer myReplyCount = fanCommentReplyMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanCommentReply>()
                                .eq("comment_id", c.getId())
                                .eq("reply_to_user_id", myUserId)
                ).intValue();
                vo.setMyReplyCount(myReplyCount);
            } else {
                vo.setMyReplyCount(0);
            }

            com.football.entity.SysUser user = sysUserMapper.selectById(c.getUserId());
            vo.setUsername(user != null ? user.getUsername() : null);
            vo.setAvatar(user != null ? user.getAvatar() : null);

            vos.add(vo);
        }

        // 存入 Redis 缓存 (2分钟过期，评论更新频繁，缓存时间不宜过长)
        try {
            redisUtil.set(cacheKey, vos, 120);
            System.out.println("比赛评论已缓存: matchId=" + matchId + ", count=" + vos.size());
        } catch (Exception e) {
            System.err.println("Redis 缓存存储失败，不影响业务: " + e.getMessage());
        }

        return Result.success(vos);
    }

    /**
     * 发布某场比赛评论（类懂球帝“评论区”）
     */
    @PostMapping("/match/{matchId}/comment")
    public Result<Boolean> addMatchComment(
            @PathVariable Long matchId,
            @RequestBody FanCommentCreateDTO dto,
            HttpServletRequest request
    ) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = auth.replace("Bearer ", "");
        Long fanUserId = JwtUtil.getUserIdFromToken(token);

        FanComment comment = new FanComment();
        comment.setMatchId(matchId);
        comment.setUserId(fanUserId);
        comment.setContent(dto.getContent());
        comment.setLikes(0);
        comment.setCreateTime(new Date());

        fanCommentMapper.insert(comment);
        
        // 清除相关缓存
        try {
            redisUtil.delete("fan:comments:match:" + matchId);
            redisUtil.deleteByPattern("fan:comments:hot*");
            System.out.println("比赛评论已添加，相关缓存已清除: matchId=" + matchId);
        } catch (Exception e) {
            System.err.println("Redis 缓存清除失败，不影响业务: " + e.getMessage());
        }
        
        return Result.success(true);
    }

    /**
     * 获取某条评论的回复列表（包含“回复回复”）
     */
    @GetMapping("/comment/{commentId}/replies")
    public Result<List<FanCommentReplyVO>> listCommentReplies(@PathVariable Long commentId) {
        try {
            List<FanCommentReply> replies = fanCommentReplyMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanCommentReply>()
                            .eq("comment_id", commentId)
                            .orderByAsc("create_time")
            );

            if (replies == null) replies = new ArrayList<>();

            List<FanCommentReplyVO> vos = new ArrayList<>();
            for (FanCommentReply r : replies) {
                FanCommentReplyVO vo = new FanCommentReplyVO();
                vo.setId(r.getId());
                vo.setCommentId(r.getCommentId());
                vo.setParentReplyId(r.getParentReplyId());
                vo.setReplyToUserId(r.getReplyToUserId());
                vo.setContent(r.getContent());
                vo.setCreateTime(r.getCreateTime());

                com.football.entity.SysUser replyUser = sysUserMapper.selectById(r.getReplyUserId());
                vo.setReplyUserId(r.getReplyUserId());
                vo.setReplyUsername(replyUser != null ? replyUser.getUsername() : null);

                com.football.entity.SysUser toUser = null;
                if (r.getReplyToUserId() != null) {
                    toUser = sysUserMapper.selectById(r.getReplyToUserId());
                }
                vo.setReplyToUsername(toUser != null ? toUser.getUsername() : null);

                // 兼容旧字段：username 直接填回复者
                vo.setUsername(vo.getReplyUsername());
                vos.add(vo);
            }
            return Result.success(vos);
        } catch (Exception e) {
            return Result.error("获取回复失败：" + e.getMessage());
        }
    }

    /**
     * 发布回复
     * body:
     * - content: String
     * - parentReplyId: Long (可选)
     * - replyToUserId: Long (可选，缺省会自动取被回复对象)
     */
    @PostMapping("/comment/{commentId}/reply")
    public Result<Boolean> replyToComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long fanUserId = JwtUtil.getUserIdFromToken(token);

            String content = body.get("content") == null ? null : String.valueOf(body.get("content")).trim();
            if (content == null || content.isEmpty()) {
                return Result.error("回复内容不能为空");
            }

            Object parentObj = body.get("parentReplyId");
            Long parentReplyId = parentObj == null ? null : Long.valueOf(parentObj.toString());

            Object toObj = body.get("replyToUserId");
            Long replyToUserId = toObj == null ? null : Long.valueOf(toObj.toString());

            // 缺省 replyToUserId：顶级回复 -> comment作者；对回复的回复 -> parent作者
            if (replyToUserId == null) {
                if (parentReplyId != null) {
                    FanCommentReply parent = fanCommentReplyMapper.selectById(parentReplyId);
                    if (parent != null) replyToUserId = parent.getReplyUserId();
                }
                if (replyToUserId == null) {
                    FanComment comment = fanCommentMapper.selectById(commentId);
                    if (comment != null) replyToUserId = comment.getUserId();
                }
            }

            FanCommentReply reply = new FanCommentReply();
            reply.setCommentId(commentId);
            reply.setParentReplyId(parentReplyId);
            reply.setReplyToUserId(replyToUserId);
            reply.setReplyUserId(fanUserId);
            reply.setContent(content);
            reply.setCreateTime(new Date());

            fanCommentReplyMapper.insert(reply);
            
            // 清除相关缓存
            try {
                FanComment comment = fanCommentMapper.selectById(commentId);
                if (comment != null && comment.getMatchId() != null) {
                    redisUtil.delete("fan:comments:match:" + comment.getMatchId());
                }
                System.out.println("评论回复已添加，相关缓存已清除: commentId=" + commentId);
            } catch (Exception e) {
                System.err.println("Redis 缓存清除失败，不影响业务: " + e.getMessage());
            }
            
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("发布回复失败：" + e.getMessage());
        }
    }

    @GetMapping("/match/{matchId}/rating/summary")
    public Result<Object> getMatchRatingSummary(@PathVariable Long matchId) {
        List<MatchRating> list = matchRatingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MatchRating>()
                        .eq("match_id", matchId)
        );
        int count = list.size();
        double avg = 0;
        if (count > 0) {
            avg = list.stream().mapToInt(r -> r.getStars() == null ? 0 : r.getStars()).average().orElse(0);
        }
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("count", count);
        data.put("avg", avg);
        return Result.success(data);
    }

    @GetMapping("/match/{matchId}/rating/me")
    public Result<MatchRating> getMyMatchRating(@PathVariable Long matchId, HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        Long fanUserId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
        MatchRating rating = matchRatingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MatchRating>()
                        .eq("match_id", matchId)
                        .eq("fan_user_id", fanUserId)
        );
        return Result.success(rating);
    }

    @PostMapping("/match/{matchId}/rating")
    public Result<Boolean> rateMatch(
            @PathVariable Long matchId,
            @RequestBody java.util.Map<String, Object> body,
            HttpServletRequest request
    ) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            Long fanUserId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            Integer stars = Integer.parseInt(body.getOrDefault("stars", 0).toString());
            String commentText = body.getOrDefault("comment", "").toString();
            if (stars < 1 || stars > 5) {
                return Result.error("评分必须为1-5星");
            }

            MatchRating existing = matchRatingMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MatchRating>()
                            .eq("match_id", matchId)
                            .eq("fan_user_id", fanUserId)
            );
            if (existing == null) {
                MatchRating rating = new MatchRating();
                rating.setMatchId(matchId);
                rating.setFanUserId(fanUserId);
                rating.setStars(stars);
                rating.setComment(commentText);
                rating.setCreateTime(new Date());
                matchRatingMapper.insert(rating);
            } else {
                existing.setStars(stars);
                existing.setComment(commentText);
                matchRatingMapper.updateById(existing);
            }
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("评分失败：" + e.getMessage());
        }
    }

    @PutMapping("/like/{commentId}")
    public Result<Boolean> likeComment(@PathVariable Long commentId) {
        FanComment comment = fanCommentMapper.selectById(commentId);
        if (comment != null) {
            comment.setLikes(comment.getLikes() + 1);
            fanCommentMapper.updateById(comment);
            
            // 清除相关缓存
            try {
                if (comment.getMatchId() != null) {
                    redisUtil.delete("fan:comments:match:" + comment.getMatchId());
                }
                redisUtil.deleteByPattern("fan:comments:hot*");
                System.out.println("评论点赞已更新，相关缓存已清除: commentId=" + commentId);
            } catch (Exception e) {
                System.err.println("Redis 缓存清除失败，不影响业务: " + e.getMessage());
            }
            
            return Result.success(true);
        }
        return Result.error("评论不存在");
    }

    @GetMapping("/news/list")
    public Result<Object> getNewsList() {
        // 实现资讯列表逻辑
        return Result.success(null);
    }

    @GetMapping("/follow/club/list")
    public Result<List<TeamClub>> getFollowedClubs(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        String token = auth.replace("Bearer ", "");
        Long fanUserId = JwtUtil.getUserIdFromToken(token);

        List<FanFollowClub> follows = fanFollowClubMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanFollowClub>()
                        .eq("fan_user_id", fanUserId)
        );

        // 依次取出俱乐部信息（避免多表 join，保持代码简单）
        List<TeamClub> clubs = follows.stream().map(f -> teamClubMapper.selectById(f.getTeamClubId())).toList();
        return Result.success(clubs);
    }

    @PostMapping("/follow/club/{clubId}")
    public Result<Boolean> followClub(@PathVariable Long clubId, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long fanUserId = JwtUtil.getUserIdFromToken(token);

            FanFollowClub existing = fanFollowClubMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanFollowClub>()
                            .eq("fan_user_id", fanUserId)
                            .eq("team_club_id", clubId)
            );
            if (existing != null) {
                return Result.success(true);
            }

            FanFollowClub follow = new FanFollowClub();
            follow.setFanUserId(fanUserId);
            follow.setTeamClubId(clubId);
            follow.setCreateTime(new Date());
            fanFollowClubMapper.insert(follow);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("关注失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/follow/club/{clubId}")
    public Result<Boolean> unfollowClub(@PathVariable Long clubId, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long fanUserId = JwtUtil.getUserIdFromToken(token);

            int deleted = fanFollowClubMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanFollowClub>()
                            .eq("fan_user_id", fanUserId)
                            .eq("team_club_id", clubId)
            );
            return Result.success(deleted > 0);
        } catch (Exception e) {
            return Result.error("取消关注失败：" + e.getMessage());
        }
    }

    // 新增：主队选择接口
    @PostMapping("/team/setMain")
    public Result<Boolean> setMainTeam(@RequestParam Long clubId, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long fanUserId = JwtUtil.getUserIdFromToken(token);

            // 先删除现有的主队关系
            fanTeamRelationMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanTeamRelation>()
                            .eq("fan_id", fanUserId)
            );

            // 创建新的主队关系
            FanTeamRelation relation = new FanTeamRelation();
            relation.setFanId(fanUserId);
            relation.setClubId(clubId);
            relation.setCreateTime(new Date());
            fanTeamRelationMapper.insert(relation);

            return Result.success(true);
        } catch (Exception e) {
            return Result.error("设置主队失败：" + e.getMessage());
        }
    }

    // 新增：资料修改接口
    @PutMapping("/profile/update")
    public Result<Boolean> updateProfile(@RequestBody com.football.entity.SysUser user, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            // 验证用户是否存在
            com.football.entity.SysUser existingUser = sysUserMapper.selectById(userId);
            if (existingUser == null) {
                return Result.error("用户不存在");
            }

            // 验证昵称唯一性
            if (user.getNickname() != null && !user.getNickname().equals(existingUser.getNickname())) {
                com.football.entity.SysUser userWithSameNickname = sysUserMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.football.entity.SysUser>()
                                .eq("nickname", user.getNickname())
                );
                if (userWithSameNickname != null) {
                    return Result.error("昵称已被使用");
                }
                existingUser.setNickname(user.getNickname());
            }

            // 更新头像
            if (user.getAvatar() != null) {
                existingUser.setAvatar(user.getAvatar());
            }

            sysUserMapper.updateById(existingUser);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("更新资料失败：" + e.getMessage());
        }
    }

    // 新增：赛事评分接口
    @PostMapping("/match/score")
    public Result<Boolean> scoreMatch(@RequestBody java.util.Map<String, Object> params, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long fanUserId = JwtUtil.getUserIdFromToken(token);

            Long matchId = Long.valueOf(params.get("matchId").toString());
            Integer starScore = Integer.valueOf(params.get("starScore").toString());

            // 验证评分范围
            if (starScore < 1 || starScore > 5) {
                return Result.error("评分必须在1-5星之间");
            }

            // 检查是否已经评分
            MatchScore existingScore = matchScoreMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MatchScore>()
                            .eq("fan_id", fanUserId)
                            .eq("match_id", matchId)
            );

            if (existingScore != null) {
                // 更新评分
                existingScore.setStarScore(starScore);
                matchScoreMapper.updateById(existingScore);
            } else {
                // 创建新评分
                MatchScore score = new MatchScore();
                score.setFanId(fanUserId);
                score.setMatchId(matchId);
                score.setStarScore(starScore);
                score.setCreateTime(new Date());
                matchScoreMapper.insert(score);
            }

            return Result.success(true);
        } catch (Exception e) {
            return Result.error("评分失败：" + e.getMessage());
        }
    }

    // 已迁移到更简洁的点赞接口：PUT /api/fan/like/{commentId}

    // 新增：获取个人资料
    @GetMapping("/profile/get")
    public Result<com.football.entity.SysUser> getProfile(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            com.football.entity.SysUser user = sysUserMapper.selectById(userId);
            if (user != null) {
                return Result.success(user);
            }
            return Result.error("用户不存在");
        } catch (Exception e) {
            return Result.error("获取个人资料失败：" + e.getMessage());
        }
    }

    // 新增：上传头像
    @PostMapping("/profile/uploadAvatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            // 检查文件类型
            String contentType = file.getContentType();
            if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
                return Result.error("只能上传JPG或PNG格式的图片");
            }

            // 检查文件大小
            if (file.getSize() > 2 * 1024 * 1024) {
                return Result.error("图片大小不能超过2MB");
            }

            // 生成文件名
            String fileName = userId + "_" + System.currentTimeMillis() + "." + contentType.split("/")[1];
            String filePath = "D:/uploads/avatars/" + fileName;

            // 创建目录
            File dest = new File(filePath);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            // 保存文件
            file.transferTo(dest);

            // 更新用户头像
            com.football.entity.SysUser user = sysUserMapper.selectById(userId);
            if (user != null) {
                user.setAvatar("/uploads/avatars/" + fileName);
                sysUserMapper.updateById(user);
                
                // 清除用户信息缓存
                try {
                    redisUtil.delete("user:info:" + userId);
                    System.out.println("用户头像更新，缓存已清除: userId=" + userId);
                } catch (Exception e) {
                    System.err.println("Redis 缓存清除失败，不影响业务: " + e.getMessage());
                }
            }

            return Result.success("/uploads/avatars/" + fileName);
        } catch (Exception e) {
            return Result.error("上传头像失败：" + e.getMessage());
        }
    }

    // 新增：获取用户对赛事的评分
    @GetMapping("/match/score/{matchId}")
    public Result<MatchScore> getMatchScore(@PathVariable Long matchId, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            MatchScore score = matchScoreMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MatchScore>()
                            .eq("fan_id", userId)
                            .eq("match_id", matchId)
            );

            if (score != null) {
                return Result.success(score);
            }
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("获取评分失败：" + e.getMessage());
        }
    }

    // 新增：获取赛事的平均评分
    @GetMapping("/match/averageScore/{matchId}")
    public Result<Object> getAverageScore(@PathVariable Long matchId) {
        try {
            List<MatchScore> scores = matchScoreMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MatchScore>()
                            .eq("match_id", matchId)
            );

            if (scores != null && !scores.isEmpty()) {
                double averageScore = scores.stream().mapToInt(MatchScore::getStarScore).average().orElse(0);
                return Result.success(new java.util.HashMap<String, Object>() {
                    {
                        put("averageScore", Math.round(averageScore * 10) / 10.0);
                        put("count", scores.size());
                    }
                });
            }
            return Result.success(new java.util.HashMap<String, Object>() {
                {
                    put("averageScore", 0);
                    put("count", 0);
                }
            });
        } catch (Exception e) {
            return Result.error("获取平均评分失败：" + e.getMessage());
        }
    }

    // 新增：获取评论列表
    @GetMapping("/comment/list")
    public Result<Object> getCommentList(
            @RequestParam Long matchId,
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize
    ) {
        try {
            // 计算偏移量
            int offset = (pageNum - 1) * pageSize;

            // 查询评论列表
            List<FanComment> comments = fanCommentMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanComment>()
                            .eq("match_id", matchId)
                            .orderByDesc("create_time")
                            .last("LIMIT " + pageSize + " OFFSET " + offset)
            );

            // 查询总条数
            long total = fanCommentMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanComment>()
                            .eq("match_id", matchId)
            );

            // 转换为VO
            List<FanCommentVO> commentVOs = new ArrayList<>();
            for (FanComment comment : comments) {
                FanCommentVO vo = new FanCommentVO();
                vo.setId(comment.getId());
                vo.setMatchId(comment.getMatchId());
                vo.setUserId(comment.getUserId());
                vo.setContent(comment.getContent());
                vo.setLikes(comment.getLikes());
                vo.setCreateTime(comment.getCreateTime());

                // 获取用户信息
                com.football.entity.SysUser user = sysUserMapper.selectById(comment.getUserId());
                if (user != null) {
                    vo.setUsername(user.getUsername());
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                }

                commentVOs.add(vo);
            }

            return Result.success(new java.util.HashMap<String, Object>() {
                {
                    put("list", commentVOs);
                    put("total", total);
                }
            });
        } catch (Exception e) {
            return Result.error("获取评论列表失败：" + e.getMessage());
        }
    }

    // 新增：添加评论
    @PostMapping("/comment/add")
    public Result<Boolean> addComment(@RequestBody FanCommentCreateDTO dto, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            FanComment comment = new FanComment();
            comment.setMatchId(dto.getMatchId());
            comment.setUserId(userId);
            comment.setContent(dto.getContent());
            comment.setLikes(0);
            comment.setCreateTime(new Date());

            fanCommentMapper.insert(comment);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("添加评论失败：" + e.getMessage());
        }
    }

    // 已迁移到更简洁的点赞接口：PUT /api/fan/like/{commentId}

    // 新增：获取我的主队
    @GetMapping("/team/main")
    public Result<TeamClub> getMainTeam(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long fanUserId = JwtUtil.getUserIdFromToken(token);

            FanTeamRelation relation = fanTeamRelationMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanTeamRelation>()
                            .eq("fan_id", fanUserId)
            );

            if (relation != null) {
                TeamClub club = teamClubMapper.selectById(relation.getClubId());
                return Result.success(club);
            }
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("获取主队失败：" + e.getMessage());
        }
    }

    // 新增：移除主队
    @DeleteMapping("/team/main")
    public Result<Boolean> removeMainTeam(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long fanUserId = JwtUtil.getUserIdFromToken(token);

            // 删除主队关系
            int deleted = fanTeamRelationMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FanTeamRelation>()
                            .eq("fan_id", fanUserId)
            );

            return Result.success(deleted > 0);
        } catch (Exception e) {
            return Result.error("移除主队失败：" + e.getMessage());
        }
    }

    @Autowired
    private com.football.mapper.FanTeamRelationMapper fanTeamRelationMapper;

    @Autowired
    private com.football.mapper.MatchScoreMapper matchScoreMapper;
}

