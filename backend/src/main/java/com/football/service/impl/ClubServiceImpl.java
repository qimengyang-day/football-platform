package com.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.TeamClub;
import com.football.entity.ClubSponsor;
import com.football.entity.SysUser;
import com.football.mapper.TeamClubMapper;
import com.football.mapper.SysUserMapper;
import com.football.mapper.ClubSponsorMapper;
import com.football.service.ClubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClubServiceImpl implements ClubService {
    private static final Logger logger = LoggerFactory.getLogger(ClubServiceImpl.class);
    
    @Autowired
    private TeamClubMapper teamClubMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private ClubSponsorMapper clubSponsorMapper;

    private void syncSponsorsFromClub(TeamClub club) {
        if (club == null || club.getId() == null) return;

        // 允许为空字符串：视为清空赞助商
        String sponsorRaw = club.getSponsor();
        java.util.Set<String> sponsorSet = new HashSet<>();
        if (sponsorRaw != null) {
            for (String s : sponsorRaw.split(",")) {
                if (s != null) {
                    String t = s.trim();
                    if (!t.isEmpty()) sponsorSet.add(t);
                }
            }
        }

        // 先清空再插入，确保完全一致
        clubSponsorMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ClubSponsor>()
                        .eq("club_id", club.getId())
        );
        for (String name : sponsorSet) {
            ClubSponsor cs = new ClubSponsor();
            cs.setClubId(club.getId());
            cs.setSponsorName(name);
            clubSponsorMapper.insert(cs);
        }
    }

    @Override
    public Page<TeamClub> getClubList(int pageNum, int pageSize) {
        Page<TeamClub> page = new Page<>(pageNum, pageSize);
        teamClubMapper.selectPage(page, null);
        return page;
    }

    @Override
    public Map<String, Object> getClubs(String search, int pageNum, int pageSize) {
        Page<TeamClub> page = new Page<>(pageNum, pageSize);
        QueryWrapper<TeamClub> queryWrapper = new QueryWrapper<>();
        if (search != null && !search.equals("")) {
            queryWrapper.like("name", search);
        }
        teamClubMapper.selectPage(page, queryWrapper);
        
        // 查询所有俱乐部用户
        List<SysUser> clubUsers = sysUserMapper.selectList(new QueryWrapper<SysUser>().eq("role", "CLUB"));
        
        // 将俱乐部用户与俱乐部关联
        List<Map<String, Object>> clubList = page.getRecords().stream().map(club -> {
            Map<String, Object> clubMap = new HashMap<>();
            clubMap.put("id", club.getId());
            clubMap.put("name", club.getName());
            clubMap.put("logo", club.getLogo());
            clubMap.put("managerId", club.getManagerId());
            clubMap.put("description", club.getDescription());
            clubMap.put("headCoach", club.getHeadCoach());
            clubMap.put("translator", club.getTranslator());
            clubMap.put("sponsor", club.getSponsor());
            clubMap.put("coachValue", club.getCoachValue());
            clubMap.put("createByAdmin", club.getCreateByAdmin());
            // 状态在当前数据模型中没有落库字段，这里统一返回 1 以兼容前端展示
            clubMap.put("status", 1);
            
            // 查找关联的俱乐部用户
            SysUser clubUser = clubUsers.stream()
                .filter(user -> user.getMainTeamId() != null && user.getMainTeamId().equals(club.getId()))
                .findFirst()
                .orElse(null);
            
            if (clubUser != null) {
                clubMap.put("username", clubUser.getUsername());
                clubMap.put("role", clubUser.getRole());
            } else {
                clubMap.put("username", "");
                clubMap.put("role", "CLUB");
            }
            
            return clubMap;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", clubList);
        result.put("total", page.getTotal());
        return result;
    }

    @Override
    public boolean addClub(TeamClub club, Long adminId) {
        logger.info("服务层添加俱乐部：name={}, managerId={}, createByAdmin={}", club.getName(), club.getManagerId(), adminId);
        club.setCreateByAdmin(adminId);
        // 唯一映射：同一 club role（managerId）只能绑定一支俱乐部
        // 注意：只有当 managerId 不为 null 时才检查唯一性
        if (club.getManagerId() != null) {
            Long count = teamClubMapper.selectCount(
                    new QueryWrapper<TeamClub>().eq("manager_id", club.getManagerId())
            );
            logger.info("检查 managerId={} 的唯一性，count={}", club.getManagerId(), count);
            if (count != null && count > 0) {
                logger.warn("俱乐部添加失败：managerId={} 已被使用", club.getManagerId());
                return false;
            }
        }
        int result = teamClubMapper.insert(club);
        logger.info("插入俱乐部结果：result={}, generated id={}", result, club.getId());
        if (result > 0) {
            // 同步赞助商到 club_sponsor，保证俱乐部端多值读取可用
            syncSponsorsFromClub(club);
            logger.info("俱乐部添加成功");
            return true;
        }
        logger.error("俱乐部添加失败：数据库插入返回{}", result);
        return false;
    }

    @Override
    public boolean updateClub(TeamClub club) {
        // 更新时同样检查 managerId 唯一性（避免 manager 绑定多支俱乐部）
        if (club.getManagerId() != null && club.getId() != null) {
            Long count = teamClubMapper.selectCount(
                    new QueryWrapper<TeamClub>()
                            .eq("manager_id", club.getManagerId())
                            .ne("id", club.getId())
            );
            if (count != null && count > 0) {
                return false;
            }
        }
        int result = teamClubMapper.updateById(club);
        if (result > 0) {
            // 同步赞助商到 club_sponsor（仅当前端传了 sponsor 字段时）
            if (club.getSponsor() != null) {
                syncSponsorsFromClub(club);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteClub(Long id) {
        int result = teamClubMapper.deleteById(id);
        return result > 0;
    }
}