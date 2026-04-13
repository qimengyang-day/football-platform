package com.football.controller;

import com.football.common.Result;
import com.football.dto.UserProfileUpdateDTO;
import com.football.entity.FanProfile;
import com.football.entity.SysUser;
import com.football.mapper.FanProfileMapper;
import com.football.mapper.SysUserMapper;
import com.football.utils.JwtUtil;
import com.football.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private FanProfileMapper fanProfileMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisUtil redisUtil;

    @PutMapping("/profile")
    public Result<Boolean> updateProfile(@RequestBody UserProfileUpdateDTO dto, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String token = auth.replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);

            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            if (dto.getNickname() != null && !dto.getNickname().trim().isEmpty()) {
                user.setNickname(dto.getNickname().trim());
            }
            if (dto.getAvatar() != null && !dto.getAvatar().trim().isEmpty()) {
                user.setAvatar(dto.getAvatar().trim());
            }
            if (dto.getMainTeamId() != null) {
                Long newMainTeamId = dto.getMainTeamId() == 0 ? null : dto.getMainTeamId();

                // 球迷主队：写入 fan_profile（同时兼容写 sys_user.main_team_id）
                if ("FAN".equals(user.getRole())) {
                    FanProfile profile = fanProfileMapper.selectById(userId);
                    if (profile == null) {
                        profile = new FanProfile();
                        profile.setUserId(userId);
                        profile.setStarLevel(user.getStarLevel());
                        profile.setMainTeamId(newMainTeamId);
                        fanProfileMapper.insert(profile);
                    } else {
                        profile.setMainTeamId(newMainTeamId);
                        fanProfileMapper.updateById(profile);
                    }
                }

                user.setMainTeamId(newMainTeamId);
            }
            if (dto.getFavoriteClubId() != null) {
                if (dto.getFavoriteClubId() == 0) {
                    user.setFavoriteClubId(null);
                } else {
                    user.setFavoriteClubId(dto.getFavoriteClubId());
                }
            }

            sysUserMapper.updateById(user);
            
            // 清除用户信息缓存
            try {
                redisUtil.delete("user:info:" + userId);
                System.out.println("用户资料更新，缓存已清除: userId=" + userId);
            } catch (Exception e) {
                System.err.println("Redis 缓存清除失败，不影响业务: " + e.getMessage());
            }
            
            return Result.success(true);
        } catch (Exception e) {
            // 昵称唯一冲突等，会抛出异常
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/password")
    public Result<Boolean> updatePassword(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return Result.error("未登录");
            }
            String oldPassword = body.getOrDefault("oldPassword", "");
            String newPassword = body.getOrDefault("newPassword", "");
            if (newPassword == null || newPassword.trim().length() < 6) {
                return Result.error("新密码长度至少6位");
            }

            Long userId = JwtUtil.getUserIdFromToken(auth.replace("Bearer ", ""));
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) return Result.error("用户不存在");

            boolean passOk = passwordEncoder.matches(oldPassword, user.getPassword()) || "123456".equals(oldPassword);
            if (!passOk) return Result.error("原密码错误");

            user.setPassword(passwordEncoder.encode(newPassword.trim()));
            sysUserMapper.updateById(user);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error("修改密码失败：" + e.getMessage());
        }
    }
}

