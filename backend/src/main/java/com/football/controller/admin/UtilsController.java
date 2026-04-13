package com.football.controller.admin;

import com.football.common.Result;
import com.football.entity.PlayerInfo;
import com.football.entity.SysUser;
import com.football.mapper.PlayerInfoMapper;
import com.football.mapper.SysUserMapper;
import com.football.utils.PinyinUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户名拼音更新管理接口
 */
@RestController
@RequestMapping("/api/admin/utils")
public class UtilsController {
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private PlayerInfoMapper playerInfoMapper;
    
    /**
     * 批量更新球员和球迷用户名为拼音格式
     */
    @PostMapping("/update-all-username-to-pinyin")
    public Result<Object> updateAllUsernameToPinyin() {
        try {
            System.out.println("=== 开始更新所有用户（球员 + 球迷）用户名为拼音格式 ===");
            
            // 收集所有已存在的用户名，避免重复
            Set<String> existingUsernames = new HashSet<>();
            List<SysUser> allUsers = sysUserMapper.selectList(null);
            for (SysUser user : allUsers) {
                if (user.getUsername() != null) {
                    existingUsernames.add(user.getUsername().toLowerCase());
                }
            }
            
            int updatedCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            
            // 更新球员用户名
            System.out.println("\n--- 开始更新球员用户名 ---");
            updatedCount += updatePlayersToPinyin(existingUsernames);
            
            // 更新球迷用户名
            System.out.println("\n--- 开始更新球迷用户名 ---");
            updatedCount += updateFansToPinyin(existingUsernames);
            
            System.out.println("\n=== 更新完成 ===");
            System.out.println("成功更新：" + updatedCount + " 个用户");
            System.out.println("跳过：" + skipCount + " 个用户");
            System.out.println("失败：" + errorCount + " 个用户");
            System.out.println("================");
            
            java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
            resultMap.put("updatedCount", updatedCount);
            resultMap.put("message", "成功更新 " + updatedCount + " 个用户名为拼音格式");
            
            return Result.success(resultMap);
            
        } catch (Exception e) {
            System.err.println("更新用户名异常：" + e.getMessage());
            e.printStackTrace();
            return Result.error("更新用户名失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新球员用户名为拼音
     */
    private int updatePlayersToPinyin(Set<String> existingUsernames) {
        List<PlayerInfo> players = playerInfoMapper.selectList(null);
        int updatedCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        
        for (PlayerInfo player : players) {
            if (player.getUserId() == null || player.getRealName() == null) {
                continue;
            }
            
            SysUser user = sysUserMapper.selectById(player.getUserId());
            if (user == null) {
                continue;
            }
            
            // 只更新角色为 PLAYER 的用户
            if (!"PLAYER".equals(user.getRole())) {
                continue;
            }
            
            // 将中文姓名转换为拼音
            String pinyinUsername = PinyinUtil.chineseToPinyin(player.getRealName());
            if (pinyinUsername == null || pinyinUsername.isEmpty()) {
                System.out.println("跳过（无法转换拼音）: " + player.getRealName());
                skipCount++;
                continue;
            }
            
            // 生成唯一的用户名（处理重复）
            String uniqueUsername = PinyinUtil.generateUniqueUsername(pinyinUsername, existingUsernames);
            
            // 如果用户名没有变化，跳过
            if (uniqueUsername.equals(user.getUsername())) {
                skipCount++;
                continue;
            }
            
            // 更新用户名
            String oldUsername = user.getUsername();
            user.setUsername(uniqueUsername);
            int result = sysUserMapper.updateById(user);
            
            if (result > 0) {
                System.out.println("更新成功（球员）：" + player.getRealName() + " [" + oldUsername + "] -> " + uniqueUsername);
                updatedCount++;
                // 添加到已存在用户名集合
                existingUsernames.add(uniqueUsername);
            } else {
                System.out.println("更新失败（球员）：" + player.getRealName());
                errorCount++;
            }
        }
        
        System.out.println("球员更新统计：成功=" + updatedCount + ", 跳过=" + skipCount + ", 失败=" + errorCount);
        return updatedCount;
    }
    
    /**
     * 更新球迷用户名为拼音
     */
    private int updateFansToPinyin(Set<String> existingUsernames) {
        // 查询所有球迷用户（有 nickname 且不为空）
        List<SysUser> fans = sysUserMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                .eq("role", "FAN")
                .isNotNull("nickname")
                .ne("nickname", "")
        );
        
        int updatedCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        
        for (SysUser fan : fans) {
            if (fan.getNickname() == null || fan.getNickname().trim().isEmpty()) {
                continue;
            }
            
            // 将中文昵称转换为拼音
            String pinyinUsername = PinyinUtil.chineseToPinyin(fan.getNickname());
            if (pinyinUsername == null || pinyinUsername.isEmpty()) {
                System.out.println("跳过（无法转换拼音）: " + fan.getNickname());
                skipCount++;
                continue;
            }
            
            // 生成唯一的用户名（处理重复）
            String uniqueUsername = PinyinUtil.generateUniqueUsername(pinyinUsername, existingUsernames);
            
            // 如果用户名没有变化，跳过
            if (uniqueUsername.equals(fan.getUsername())) {
                skipCount++;
                continue;
            }
            
            // 更新用户名
            String oldUsername = fan.getUsername();
            fan.setUsername(uniqueUsername);
            int result = sysUserMapper.updateById(fan);
            
            if (result > 0) {
                System.out.println("更新成功（球迷）：" + fan.getNickname() + " [" + oldUsername + "] -> " + uniqueUsername);
                updatedCount++;
                // 添加到已存在用户名集合
                existingUsernames.add(uniqueUsername);
            } else {
                System.out.println("更新失败（球迷）：" + fan.getNickname());
                errorCount++;
            }
        }
        
        System.out.println("球迷更新统计：成功=" + updatedCount + ", 跳过=" + skipCount + ", 失败=" + errorCount);
        return updatedCount;
    }
    
    /**
     * 预览将要更新的用户名（不实际执行更新）
     */
    @PostMapping("/preview-player-username-update")
    public Result<Object> previewPlayerUsernameUpdate() {
        try {
            // 收集所有已存在的用户名
            Set<String> existingUsernames = new HashSet<>();
            List<SysUser> allUsers = sysUserMapper.selectList(null);
            for (SysUser user : allUsers) {
                if (user.getUsername() != null) {
                    existingUsernames.add(user.getUsername().toLowerCase());
                }
            }
            
            // 获取所有球员信息
            List<PlayerInfo> players = playerInfoMapper.selectList(null);
            java.util.List<Map<String, String>> previewList = new java.util.ArrayList<>();
            
            for (PlayerInfo player : players) {
                if (player.getUserId() == null || player.getRealName() == null) {
                    continue;
                }
                
                SysUser user = sysUserMapper.selectById(player.getUserId());
                if (user == null) {
                    continue;
                }
                
                // 只更新角色为 PLAYER 的用户
                if (!"PLAYER".equals(user.getRole())) {
                    continue;
                }
                
                // 将中文姓名转换为拼音
                String pinyinUsername = PinyinUtil.chineseToPinyin(player.getRealName());
                if (pinyinUsername == null || pinyinUsername.isEmpty()) {
                    continue;
                }
                
                // 生成唯一的用户名（处理重复）
                String uniqueUsername = PinyinUtil.generateUniqueUsername(pinyinUsername, existingUsernames);
                
                Map<String, String> preview = new java.util.HashMap<>();
                preview.put("realName", player.getRealName());
                preview.put("oldUsername", user.getUsername());
                preview.put("newUsername", uniqueUsername);
                preview.put("willUpdate", uniqueUsername.equals(user.getUsername()) ? "否" : "是");
                
                previewList.add(preview);
                
                // 添加到已存在用户名集合（用于模拟后续重复检测）
                if (!uniqueUsername.equals(user.getUsername())) {
                    existingUsernames.add(uniqueUsername);
                }
            }
            
            return Result.success(previewList);
            
        } catch (Exception e) {
            System.err.println("预览用户名更新异常：" + e.getMessage());
            e.printStackTrace();
            return Result.error("预览用户名更新失败：" + e.getMessage());
        }
    }
}
