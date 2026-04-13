package com.football;

import com.football.entity.PlayerInfo;
import com.football.entity.SysUser;
import com.football.mapper.PlayerInfoMapper;
import com.football.mapper.SysUserMapper;
import com.football.utils.PinyinUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 更新用户名为拼音格式的命令行运行器
 */
@Component
public class UpdateUsernameToPinyinRunner implements CommandLineRunner {
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private PlayerInfoMapper playerInfoMapper;
    
    // 设置为 false 以禁用自动执行
    private static final boolean ENABLED = false;
    
    @Override
    public void run(String... args) throws Exception {
        if (!ENABLED) {
            System.out.println("=== 跳过用户名拼音更新（ENABLED = false）===");
            return;
        }
        
        System.out.println("=== 开始更新用户名为拼音格式 ===");
        
        // 收集所有已存在的用户名，避免重复
        Set<String> existingUsernames = new HashSet<>();
        List<SysUser> allUsers = sysUserMapper.selectList(null);
        for (SysUser user : allUsers) {
            if (user.getUsername() != null) {
                existingUsernames.add(user.getUsername().toLowerCase());
            }
        }
        
        // 用于跟踪生成的用户名
        Map<String, Integer> usernameCountMap = new HashMap<>();
        
        // 获取所有球员信息
        List<PlayerInfo> players = playerInfoMapper.selectList(null);
        int updatedCount = 0;
        int skipCount = 0;
        
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
            user.setUsername(uniqueUsername);
            int result = sysUserMapper.updateById(user);
            
            if (result > 0) {
                System.out.println("更新成功：" + player.getRealName() + " -> " + uniqueUsername + 
                    " (原用户名：" + user.getUsername() + ")");
                updatedCount++;
                // 添加到已存在用户名集合
                existingUsernames.add(uniqueUsername);
            } else {
                System.out.println("更新失败：" + player.getRealName());
            }
        }
        
        System.out.println("=== 更新完成 ===");
        System.out.println("成功更新：" + updatedCount + " 个用户");
        System.out.println("跳过：" + skipCount + " 个用户");
        System.out.println("================");
    }
}
