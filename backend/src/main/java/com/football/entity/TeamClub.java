package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("team_club")
public class TeamClub {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String logo;
    
    @TableField(value = "manager_id", insertStrategy = FieldStrategy.IGNORED)
    private Long managerId;
    
    private String description;
    private String headCoach; // 主教练
    private java.math.BigDecimal coachValue; // 主教练身价/价值
    private String translator; // 翻译
    private String sponsor; // 赞助商
    private Long createByAdmin; // 创建管理员ID
}