package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("player_profile_update")
public class PlayerProfileUpdate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playerUserId;
    private String realName;
    private Integer height;
    private Integer weight;
    private String position;
    private String nationality;
    private Integer age;
    private BigDecimal marketValue;
    private String status; // PENDING/APPROVED/REJECTED
    private String adminRemark;
    private Date createTime;
    private Date auditTime;
}

