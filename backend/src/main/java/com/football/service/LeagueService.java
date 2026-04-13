package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.League;
import com.football.entity.TeamClub;

import java.util.List;
import java.util.Map;

public interface LeagueService {
    Page<League> getLeagueList(int pageNum, int pageSize);
    Map<String, Object> getLeagues(String search, int pageNum, int pageSize);
    boolean addLeague(League league);
    boolean updateLeague(League league);
    boolean deleteLeague(Long id);
    boolean addClubToLeague(Long clubId, Long leagueId);
    List<League> getAllLeagues();
    List<TeamClub> getClubsByLeagueId(Long leagueId);
}