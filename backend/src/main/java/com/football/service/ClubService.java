package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.TeamClub;

import java.util.Map;

public interface ClubService {
    Page<TeamClub> getClubList(int pageNum, int pageSize);
    Map<String, Object> getClubs(String search, int pageNum, int pageSize);
    boolean addClub(TeamClub club, Long adminId);
    boolean updateClub(TeamClub club);
    boolean deleteClub(Long id);
}