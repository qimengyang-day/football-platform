package com.football.controller.club;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.football.controller.club.ClubController;
import com.football.entity.PlayerInfo;
import com.football.entity.TeamClub;
import com.football.mapper.CoachMapper;
import com.football.mapper.FootballMatchMapper;
import com.football.mapper.PlayerApplicationMapper;
import com.football.mapper.PlayerInfoMapper;
import com.football.mapper.PlayerMapper;
import com.football.mapper.MatchRegistrationMapper;
import com.football.mapper.TeamClubMapper;
import com.football.utils.JwtUtil;
import com.football.utils.RedisUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.mockito.Mockito;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ClubControllerTest {
    @org.mockito.Mock
    private TeamClubMapper teamClubMapper;

    @org.mockito.Mock
    private PlayerInfoMapper playerInfoMapper;

    // 字段注入一致性用（该测试不需要真正使用 RedisUtil）
    @org.mockito.Mock
    private RedisUtil redisUtil;

    @org.mockito.InjectMocks
    private ClubController controller;

    @Test
    void getMyClubSummary_shouldComputeTotalValue() {
        TeamClub club = new TeamClub();
        club.setId(10L);
        club.setCoachValue(new BigDecimal("100"));

        when(teamClubMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(club));

        PlayerInfo p1 = new PlayerInfo();
        p1.setMarketValue(50.0);
        PlayerInfo p2 = new PlayerInfo();
        p2.setMarketValue(30.0);
        when(playerInfoMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(p1, p2));

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer token");

        try (MockedStatic<JwtUtil> jwt = org.mockito.Mockito.mockStatic(JwtUtil.class)) {
            jwt.when(() -> JwtUtil.getUserIdFromToken("token")).thenReturn(1L);

            com.football.common.Result<Object> res = controller.getMyClubSummary(req);
            org.junit.jupiter.api.Assertions.assertEquals(200, res.getCode());

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) res.getData();
            org.junit.jupiter.api.Assertions.assertEquals(2, data.get("playerCount"));
            double totalValue = ((Number) data.get("totalValue")).doubleValue();
            org.junit.jupiter.api.Assertions.assertEquals(180.0, totalValue, 0.0001);
        }
    }
}

