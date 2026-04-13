package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.PlayerInfo;

import java.util.Map;

public interface PlayerService {
    Page<PlayerInfo> getPlayerList(int pageNum, int pageSize);
    Map<String, Object> getPlayers(String search, Long clubId, int pageNum, int pageSize);
    boolean addPlayer(PlayerInfo playerInfo);
    boolean addPlayerWithUser(PlayerInfo playerInfo);
    boolean updatePlayer(PlayerInfo playerInfo);
    boolean deletePlayer(Long id);
}