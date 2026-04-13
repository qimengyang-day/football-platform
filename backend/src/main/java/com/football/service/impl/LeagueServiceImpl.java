package com.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.ClubLeagueRelation;
import com.football.entity.TeamClub;
import com.football.entity.League;
import com.football.mapper.ClubLeagueRelationMapper;
import com.football.mapper.LeagueMapper;
import com.football.mapper.TeamClubMapper;
import com.football.service.LeagueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class LeagueServiceImpl implements LeagueService {
    @Autowired
    private LeagueMapper leagueMapper;
    @Autowired
    private ClubLeagueRelationMapper clubLeagueRelationMapper;
    @Autowired
    private TeamClubMapper teamClubMapper;

    @Override
    public Page<League> getLeagueList(int pageNum, int pageSize) {
        Page<League> page = new Page<>(pageNum, pageSize);
        leagueMapper.selectPage(page, null);
        return page;
    }

    @Override
    public Map<String, Object> getLeagues(String search, int pageNum, int pageSize) {
        Page<League> page = new Page<>(pageNum, pageSize);
        QueryWrapper<League> queryWrapper = new QueryWrapper<>();
        if (search != null && !search.equals("")) {
            queryWrapper.like("name", search);
        }
        queryWrapper.orderByDesc("id");
        leagueMapper.selectPage(page, queryWrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (League league : page.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", league.getId());
            row.put("name", league.getName());
            row.put("cover", league.getCover());
            row.put("description", league.getDescription());
            row.put("createTime", league.getCreateTime());
            Long clubCount = clubLeagueRelationMapper.selectCount(
                    new QueryWrapper<ClubLeagueRelation>().eq("league_id", league.getId())
            );
            row.put("clubCount", clubCount == null ? 0 : clubCount);
            list.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        return result;
    }

    @Override
    public boolean addLeague(League league) {
        if (league.getCreateTime() == null) {
            league.setCreateTime(new Date());
        }
        int result = leagueMapper.insert(league);
        return result > 0;
    }

    @Override
    public boolean updateLeague(League league) {
        int result = leagueMapper.updateById(league);
        return result > 0;
    }

    @Override
    public boolean deleteLeague(Long id) {
        // 删除联赛时，先清空联赛与俱乐部关联
        clubLeagueRelationMapper.delete(new QueryWrapper<ClubLeagueRelation>().eq("league_id", id));
        
        int result = leagueMapper.deleteById(id);
        return result > 0;
    }

    @Override
    public boolean addClubToLeague(Long clubId, Long leagueId) {
        ClubLeagueRelation exist = clubLeagueRelationMapper.selectOne(
                new QueryWrapper<ClubLeagueRelation>()
                        .eq("club_id", clubId)
                        .eq("league_id", leagueId)
        );
        if (exist != null) {
            return true;
        }

        ClubLeagueRelation relation = new ClubLeagueRelation();
        relation.setClubId(clubId);
        relation.setLeagueId(leagueId);
        relation.setJoinTime(new Date());
        int result = clubLeagueRelationMapper.insert(relation);
        return result > 0;
    }

    @Override
    public List<League> getAllLeagues() {
        return leagueMapper.selectList(null);
    }

    @Override
    public List<TeamClub> getClubsByLeagueId(Long leagueId) {
        // 通过 club_league_relation 表查询该联赛下的所有俱乐部
        QueryWrapper<ClubLeagueRelation> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("league_id", leagueId);
        List<ClubLeagueRelation> relations = clubLeagueRelationMapper.selectList(relationWrapper);
        
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> clubIds = new ArrayList<>();
        for (ClubLeagueRelation relation : relations) {
            clubIds.add(relation.getClubId());
        }
        
        // 根据俱乐部 ID 列表查询俱乐部信息
        QueryWrapper<TeamClub> clubWrapper = new QueryWrapper<>();
        clubWrapper.in("id", clubIds);
        return teamClubMapper.selectList(clubWrapper);
    }
}
