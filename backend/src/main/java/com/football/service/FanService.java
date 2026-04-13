package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.SysUser;

import java.util.Map;

public interface FanService {
    Page<SysUser> getFanList(int pageNum, int pageSize);
    Map<String, Object> getFans(String search, int pageNum, int pageSize);
    boolean updateFanStatus(Long userId, int status);
    SysUser getFanDetail(Long id);
}