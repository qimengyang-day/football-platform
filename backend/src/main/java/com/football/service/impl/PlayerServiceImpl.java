package com.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.PlayerInfo;
import com.football.entity.SysUser;
import com.football.entity.TeamClub;
import com.football.mapper.PlayerInfoMapper;
import com.football.mapper.SysUserMapper;
import com.football.mapper.TeamClubMapper;
import com.football.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlayerServiceImpl implements PlayerService {
    @Autowired
    private PlayerInfoMapper playerInfoMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private TeamClubMapper teamClubMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Page<PlayerInfo> getPlayerList(int pageNum, int pageSize) {
        Page<PlayerInfo> page = new Page<>(pageNum, pageSize);
        playerInfoMapper.selectPage(page, null);
        return page;
    }

    @Override
    public Map<String, Object> getPlayers(String search, Long clubId, int pageNum, int pageSize) {
        Page<PlayerInfo> page = new Page<>(pageNum, pageSize);
        QueryWrapper<PlayerInfo> queryWrapper = new QueryWrapper<>();
        if (search != null && !search.equals("")) {
            queryWrapper.like("real_name", search);
        }
        if (clubId != null && clubId > 0) {
            queryWrapper.eq("team_id", clubId);
        }
        playerInfoMapper.selectPage(page, queryWrapper);
        // 手动计算总记录数
        long total = playerInfoMapper.selectCount(queryWrapper);

        // 批量查询用户名与俱乐部名称，避免逐条查询
        Map<Long, SysUser> userMap = new HashMap<>();
        Map<Long, TeamClub> clubMap = new HashMap<>();
        List<Long> userIds = new ArrayList<>();
        List<Long> clubIds = new ArrayList<>();
        for (PlayerInfo pi : page.getRecords()) {
            if (pi.getUserId() != null && pi.getUserId() > 0) {
                userIds.add(pi.getUserId());
            }
            if (pi.getTeamId() != null && pi.getTeamId() > 0) {
                clubIds.add(pi.getTeamId());
            }
        }
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            for (SysUser u : users) {
                userMap.put(u.getId(), u);
            }
        }
        if (!clubIds.isEmpty()) {
            List<TeamClub> clubs = teamClubMapper.selectBatchIds(clubIds);
            for (TeamClub c : clubs) {
                clubMap.put(c.getId(), c);
            }
        }

        // 补齐前端表格需要的 teamName/status 显示字段
        List<Map<String, Object>> list = new ArrayList<>();
        for (PlayerInfo pi : page.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", pi.getId());
            row.put("userId", pi.getUserId());
            row.put("realName", pi.getRealName());

            // 查询用户名（批量获取）
            String username = "";
            if (pi.getUserId() != null && pi.getUserId() > 0) {
                SysUser user = userMap.get(pi.getUserId());
                if (user != null) {
                    username = user.getUsername();
                }
            }
            row.put("username", username);

            row.put("height", pi.getHeight());
            row.put("weight", pi.getWeight());
            row.put("position", pi.getPosition());
            row.put("teamId", pi.getTeamId());
            row.put("goals", pi.getGoals());
            row.put("assists", pi.getAssists());
            row.put("marketValue", pi.getMarketValue());
            row.put("nationality", pi.getNationality());
            row.put("age", pi.getAge());
            row.put("isFreeAgent", pi.getIsFreeAgent() == null ? 1 : pi.getIsFreeAgent());
            row.put("status", pi.getStatus());

            String teamName = "";
            // 只有当球员不是自由身且有 teamId 时，才显示俱乐部名称
            if (pi.getIsFreeAgent() == null || pi.getIsFreeAgent() == 0) {
                if (pi.getTeamId() != null && pi.getTeamId() > 0) {
                    TeamClub club = clubMap.get(pi.getTeamId());
                    if (club != null) {
                        teamName = club.getName();
                    }
                }
            }
            // 如果是自由身，强制 teamName 为空
            if (pi.getIsFreeAgent() != null && pi.getIsFreeAgent() == 1) {
                teamName = "";
            }
            row.put("teamName", teamName);
            list.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    @Override
    public boolean addPlayer(PlayerInfo playerInfo) {
        // 如果 userId 为 0 或 null，自动创建用户
        if (playerInfo.getUserId() == null || playerInfo.getUserId() == 0) {
            // 创建新的球员用户
            SysUser user = new SysUser();
            String username = "player" + System.currentTimeMillis();
            user.setUsername(username);
            user.setNickname(playerInfo.getRealName());
            user.setPassword(passwordEncoder.encode("123456")); // 默认密码
            user.setRole("PLAYER");
            user.setAvatar("/images/default-avatar.png");
            user.setStatus(1);
                
            sysUserMapper.insert(user);
            playerInfo.setUserId(user.getId());
        }
            
        // 俱乐部选择与状态联动：有俱乐部=已签约，无俱乐部=自由身
        if (playerInfo.getTeamId() != null && playerInfo.getTeamId() > 0) {
            playerInfo.setIsFreeAgent(0);
            playerInfo.setStatus("俱乐部 成员");
            playerInfo.setJoinStatus("已审核");
        } else {
            playerInfo.setTeamId(null);
            playerInfo.setIsFreeAgent(1);
            playerInfo.setStatus("自由身");
            playerInfo.setJoinStatus("");
        }
            
        int result = playerInfoMapper.insert(playerInfo);
        return result > 0;
    }
    
    @Override
    public boolean addPlayerWithUser(PlayerInfo playerInfo) {
        // userId 应该已经在 Controller 中设置好了
        // 俱乐部选择与状态联动：有俱乐部=已签约，无俱乐部=自由身
        if (playerInfo.getTeamId() != null && playerInfo.getTeamId() > 0) {
            playerInfo.setIsFreeAgent(0);
            playerInfo.setStatus("俱乐部 成员");
            playerInfo.setJoinStatus("已审核");
        } else {
            playerInfo.setTeamId(null);
            playerInfo.setIsFreeAgent(1);
            playerInfo.setStatus("自由身");
            playerInfo.setJoinStatus("");
        }
            
        int result = playerInfoMapper.insert(playerInfo);
        return result > 0;
    }

    @Override
    public boolean updatePlayer(PlayerInfo playerInfo) {
        // 编辑后联动状态：非自由身=已签约，自由身=自由身
        if (playerInfo.getTeamId() != null && playerInfo.getTeamId() > 0) {
            playerInfo.setIsFreeAgent(0);
            playerInfo.setStatus("俱乐部成员");
            playerInfo.setJoinStatus("已审核");
        } else {
            playerInfo.setTeamId(null);
            playerInfo.setIsFreeAgent(1);
            playerInfo.setStatus("自由身");
            playerInfo.setJoinStatus("");
        }
        int result = playerInfoMapper.updateById(playerInfo);
        return result > 0;
    }

    @Override
    public boolean deletePlayer(Long id) {
        int result = playerInfoMapper.deleteById(id);
        return result > 0;
    }
}