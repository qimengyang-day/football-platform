package com.football.controller;

import com.football.common.Result;
import com.football.dto.LoginDTO;
import com.football.dto.RegisterDTO;
import com.football.service.UserService;
import com.football.utils.JwtUtil;
import com.football.utils.RedisUtil;
import com.football.vo.LoginVO;
import com.football.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        try {
            System.out.println("Login request received: " + dto);
            LoginVO vo = userService.login(dto);
            System.out.println("Login successful: " + vo);
            return Result.success(vo);
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody RegisterDTO dto) {
        try {
            boolean success = userService.register(dto);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<UserInfoVO> getCurrentUserInfo(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(token);
        UserInfoVO userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    @PostMapping("/logout")
    public Result<Boolean> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        Long userId = JwtUtil.getUserIdFromToken(token);
        redisUtil.delete("token:" + userId);
        return Result.success(true);
    }

    /**
     * 检查用户名是否已存在
     */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.checkUsernameExists(username);
        return Result.success(exists);
    }

    /**
     * 检查昵称是否已存在
     */
    @GetMapping("/check-nickname")
    public Result<Boolean> checkNickname(@RequestParam String nickname) {
        boolean exists = userService.checkNicknameExists(nickname);
        return Result.success(exists);
    }

    /**
     * 修改密码
     */
    @PutMapping("/change-password")
    public Result<Boolean> changePassword(
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").replace("Bearer ", "");
            Long userId = JwtUtil.getUserIdFromToken(token);
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");
            
            boolean success = userService.changePassword(userId, oldPassword, newPassword);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}