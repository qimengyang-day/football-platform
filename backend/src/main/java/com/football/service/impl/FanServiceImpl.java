package com.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.SysUser;
import com.football.entity.FanProfile;
import com.football.entity.TeamClub;
import com.football.mapper.FanProfileMapper;
import com.football.mapper.SysUserMapper;
import com.football.mapper.TeamClubMapper;
import com.football.service.FanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FanServiceImpl implements FanService {
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private TeamClubMapper teamClubMapper;

    @Autowired
    private FanProfileMapper fanProfileMapper;

    @Override
    public Page<SysUser> getFanList(int pageNum, int pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        sysUserMapper.selectPage(page, new QueryWrapper<SysUser>().eq("role", "FAN"));
        return page;
    }

    @Override
    public Map<String, Object> getFans(String search, int pageNum, int pageSize) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role", "FAN");
        if (search != null && !search.equals("")) {
            // 包括括号，避免 OR 破坏 role=FAN 条件
            queryWrapper.and(w -> w.like("username", search).or().like("nickname", search));
        }

        queryWrapper.orderByDesc("id");
        sysUserMapper.selectPage(page, queryWrapper);
        // 手动计算总记录数
        long total = sysUserMapper.selectCount(queryWrapper);
        
        // 获取所有球迷用户
        List<SysUser> fans = page.getRecords();
        
        // 为每个球迷设置主队名称
        List<Map<String, Object>> fanList = new ArrayList<>();
        fans.forEach(fan -> {
            Map<String, Object> fanMap = new HashMap<>();
            fanMap.put("id", fan.getId());
            fanMap.put("username", fan.getUsername());
            fanMap.put("nickname", fan.getNickname());
            fanMap.put("avatar", fan.getAvatar());
            // starLevel/mainTeamId 来自 fan_profile
            FanProfile profile = fanProfileMapper.selectById(fan.getId());
            Integer starLevel = profile != null ? profile.getStarLevel() : fan.getStarLevel();
            Long mainTeamId = profile != null ? profile.getMainTeamId() : fan.getMainTeamId();

            fanMap.put("starLevel", starLevel);
            fanMap.put("phone", fan.getPhone());
            fanMap.put("status", fan.getStatus());
            fanMap.put("createTime", fan.getCreateTime());
            fanMap.put("lastLoginTime", fan.getLastLoginTime());
            fanMap.put("mainTeamId", mainTeamId);
            
            // 添加主队名称
            if (mainTeamId != null) {
                TeamClub team = teamClubMapper.selectById(mainTeamId);
                if (team != null) {
                    fanMap.put("mainTeamName", team.getName());
                } else {
                    fanMap.put("mainTeamName", "");
                }
            } else {
                fanMap.put("mainTeamName", "");
            }
            
            fanList.add(fanMap);
        });
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", fanList);
        result.put("total", total);
        return result;
    }

    @Override
    public boolean updateFanStatus(Long userId, int status) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        int result = sysUserMapper.updateById(user);
        return result > 0;
    }

    @Override
    public SysUser getFanDetail(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) return null;
        // 将星级/主队从 fan_profile 回填到 SysUser（保持接口返回类型不变）
        FanProfile profile = fanProfileMapper.selectById(id);
        if (profile != null) {
            user.setStarLevel(profile.getStarLevel());
            user.setMainTeamId(profile.getMainTeamId());
        }
        // 详情页需要主队名称（运行时计算）
        if (user.getMainTeamId() != null) {
            TeamClub team = teamClubMapper.selectById(user.getMainTeamId());
            user.setMainTeamName(team != null ? team.getName() : "");
        } else {
            user.setMainTeamName("");
        }
        return user;
    }
}