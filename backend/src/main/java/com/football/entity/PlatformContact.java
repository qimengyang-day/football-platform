package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("platform_contact")
public class PlatformContact {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String officialWechat;
    private String officialEmail;
    private String officialPhone;
    private String officialQq;
    private String officialWebsite;
    private String remark;

    private Date createTime;
}

