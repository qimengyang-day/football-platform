package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.PlayerClubApply;

import java.util.Map;

public interface ApplicationService {
    Page<PlayerClubApply> getPlayerApplyList(int pageNum, int pageSize);
    Map<String, Object> getApplications(Integer status, int pageNum, int pageSize);
    boolean auditPlayerApply(Long applyId, int status, Long adminId);
}