package com.football.service;

import com.football.dto.LoginDTO;
import com.football.dto.RegisterDTO;
import com.football.entity.SysUser;
import com.football.vo.LoginVO;
import com.football.vo.UserInfoVO;

public interface UserService {
    LoginVO login(LoginDTO dto);
    boolean register(RegisterDTO dto);
    UserInfoVO getUserInfo(Long userId);
    SysUser getByUsername(String username);
    boolean checkUsernameExists(String username);
    boolean checkNicknameExists(String nickname);
    boolean changePassword(Long userId, String oldPassword, String newPassword);
}