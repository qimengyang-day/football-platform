package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("league")
public class League {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String cover;
    private String description;
    private Date createTime;
}