package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.football.entity.SysUser;

public interface SysUserMapper extends BaseMapper<SysUser> {
    SysUser selectByUsername(String username);
    SysUser selectByPhone(String phone);
}
