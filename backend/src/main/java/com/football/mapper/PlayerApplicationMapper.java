package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.football.entity.PlayerApplication;

import java.util.List;

public interface PlayerApplicationMapper extends BaseMapper<PlayerApplication> {
    List<PlayerApplication> selectByClubId(Long clubId);
}
