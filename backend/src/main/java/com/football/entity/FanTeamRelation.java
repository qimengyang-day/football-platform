package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("fan_team_relation")
public class FanTeamRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fanId;
    private Long clubId;
    private Date createTime;
}