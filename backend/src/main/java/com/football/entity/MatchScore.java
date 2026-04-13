package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("match_score")
public class MatchScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fanId;
    private Long matchId;
    private Integer starScore; // 评分星级（1-5）
    private Date createTime;
}