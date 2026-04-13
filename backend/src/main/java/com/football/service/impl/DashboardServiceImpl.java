package com.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.football.mapper.*;
import com.football.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);
    
    @Autowired
    private PlayerInfoMapper playerInfoMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private TeamClubMapper teamClubMapper;
    @Autowired
    private LeagueMapper leagueMapper;
    @Autowired
    private PlayerClubApplyMapper playerClubApplyMapper;

    @Override
    public Map<String, Object> getDashboardData() {
        try {
            logger.info("开始获取仪表盘数据");
            // 统计数据
            long playerCount = playerInfoMapper.selectCount(null);
            logger.info("球员数量: {}", playerCount);
            
            long fanCount = sysUserMapper.selectCount(new QueryWrapper<com.football.entity.SysUser>().eq("role", "FAN"));
            logger.info("球迷数量: {}", fanCount);
            
            long clubCount = teamClubMapper.selectCount(null);
            logger.info("俱乐部数量: {}", clubCount);
            
            long adminCount = sysUserMapper.selectCount(new QueryWrapper<com.football.entity.SysUser>().eq("role", "ADMIN"));
            logger.info("管理员数量: {}", adminCount);
            
            long leagueCount = leagueMapper.selectCount(null);
            logger.info("联赛数量: {}", leagueCount);
            
            long pendingApplyCount = playerClubApplyMapper.selectCount(new QueryWrapper<com.football.entity.PlayerClubApply>().eq("apply_status", 0));
            logger.info("待处理申请数量: {}", pendingApplyCount);

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("playerCount", playerCount);
            data.put("fanCount", fanCount);
            data.put("clubCount", clubCount);
            data.put("adminCount", adminCount);
            data.put("leagueCount", leagueCount);
            data.put("pendingApplyCount", pendingApplyCount);

            logger.info("获取仪表盘数据成功");
            return data;
        } catch (Exception e) {
            logger.error("获取仪表盘数据失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}