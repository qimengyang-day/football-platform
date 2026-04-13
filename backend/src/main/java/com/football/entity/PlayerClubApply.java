package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("player_club_apply")
public class PlayerClubApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long playerId;
    private Long clubId;
    private Integer applyStatus; // 0=待审核 1=通过 2=拒绝
    private Date applyTime;
    private Date auditTime;
    private Long auditBy;
}