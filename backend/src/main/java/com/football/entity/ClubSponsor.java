package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("club_sponsor")
public class ClubSponsor {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 对应 team_club.id
    private Long clubId;

    private String sponsorName;
}

