package com.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.PlayerClubApply;
import com.football.entity.PlayerInfo;
import com.football.mapper.PlayerClubApplyMapper;
import com.football.mapper.PlayerInfoMapper;
import com.football.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    @Autowired
    private PlayerClubApplyMapper playerClubApplyMapper;
    @Autowired
    private PlayerInfoMapper playerInfoMapper;

    @Override
    public Page<PlayerClubApply> getPlayerApplyList(int pageNum, int pageSize) {
        Page<PlayerClubApply> page = new Page<>(pageNum, pageSize);
        playerClubApplyMapper.selectPage(page, new QueryWrapper<PlayerClubApply>().eq("apply_status", 0));
        return page;
    }

    @Override
    public Map<String, Object> getApplications(Integer status, int pageNum, int pageSize) {
        Page<PlayerClubApply> page = new Page<>(pageNum, pageSize);
        QueryWrapper<PlayerClubApply> queryWrapper = new QueryWrapper<>();
        if (status != null) {
            queryWrapper.eq("apply_status", status);
        }
        playerClubApplyMapper.selectPage(page, queryWrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    @Override
    public boolean auditPlayerApply(Long applyId, int status, Long adminId) {
        PlayerClubApply apply = playerClubApplyMapper.selectById(applyId);
        if (apply != null) {
            apply.setApplyStatus(status);
            apply.setAuditTime(new Date());
            apply.setAuditBy(adminId);
            playerClubApplyMapper.updateById(apply);

            // 如果审核通过，更新球员信息
            if (status == 1) {
                PlayerInfo playerInfo = playerInfoMapper.selectByUserId(apply.getPlayerId());
                if (playerInfo != null) {
                    playerInfo.setTeamId(apply.getClubId());
                    playerInfo.setIsFreeAgent(0);
                    playerInfo.setStatus("俱乐部成员");
                    playerInfo.setJoinStatus("已审核");
                    playerInfoMapper.updateById(playerInfo);
                }
            }
            return true;
        }
        return false;
    }
}