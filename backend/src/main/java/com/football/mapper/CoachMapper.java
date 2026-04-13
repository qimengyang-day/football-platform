package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.football.entity.Coach;

import java.util.List;

public interface CoachMapper extends BaseMapper<Coach> {
    List<Coach> selectByClubId(Long clubId);
}
