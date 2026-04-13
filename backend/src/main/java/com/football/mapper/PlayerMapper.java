package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.football.entity.Player;

import java.util.List;

public interface PlayerMapper extends BaseMapper<Player> {
    List<Player> selectByClubId(Long clubId);
}
