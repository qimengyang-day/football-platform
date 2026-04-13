package com.football.controller;

import com.football.entity.SysUser;
import com.football.mapper.SysUserMapper;
import com.football.utils.RedisUtil;
import com.football.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import com.football.common.Result;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisUtil redisUtil; // 仅为 Controller 依赖保持一致

    @InjectMocks
    private UserController controller;

    @Test
    void updatePassword_wrongOldPassword_shouldReturnError() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setPassword("hashed-old");

        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old-wrong", "hashed-old")).thenReturn(false);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer token");

        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "old-wrong");
        body.put("newPassword", "newpass123");

        try (MockedStatic<JwtUtil> jwt = org.mockito.Mockito.mockStatic(JwtUtil.class)) {
            jwt.when(() -> JwtUtil.getUserIdFromToken("token")).thenReturn(1L);

            Result<Boolean> res = controller.updatePassword(body, req);
            org.junit.jupiter.api.Assertions.assertEquals(500, res.getCode());
            org.junit.jupiter.api.Assertions.assertEquals("原密码错误", res.getMessage());
        }
    }

    @Test
    void updatePassword_correctOldPassword_shouldReturnSuccess() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setPassword("hashed-old");

        when(sysUserMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old-ok", "hashed-old")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded-newpass123");
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer token");

        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", "old-ok");
        body.put("newPassword", "newpass123");

        try (MockedStatic<JwtUtil> jwt = org.mockito.Mockito.mockStatic(JwtUtil.class)) {
            jwt.when(() -> JwtUtil.getUserIdFromToken("token")).thenReturn(1L);

            Result<Boolean> res = controller.updatePassword(body, req);
            org.junit.jupiter.api.Assertions.assertEquals(200, res.getCode());
        }
    }
}

