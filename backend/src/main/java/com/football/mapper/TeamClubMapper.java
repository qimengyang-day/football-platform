package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.football.entity.TeamClub;

import java.util.List;

public interface TeamClubMapper extends BaseMapper<TeamClub> {
    TeamClub selectByManagerId(Long managerId);
    List<TeamClub> selectRankedClubs();
}