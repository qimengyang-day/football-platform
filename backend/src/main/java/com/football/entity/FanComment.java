package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("fan_comment")
public class FanComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long matchId;
    private Long userId;
    private String content;
    private Integer likes;
    private Date createTime;
}