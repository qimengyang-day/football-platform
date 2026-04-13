package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("player_info")
public class PlayerInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String realName;
    private Integer height;
    private Integer weight;
    private String position;
    private Long teamId;
    private Integer goals;
    private Integer assists;
    private Double marketValue; // 身价（万元）
    private String nationality;
    private Integer age;
    private String phone; // 电话号
    private Integer isFreeAgent; // 是否自由身 1=是 0=否
    private String transferRecord; // 转会记录
    private String matchRecord; // 比赛记录
    private Integer reviewCount; // 球迷评价数
    private Double reviewScore; // 球迷评分（1-5）
    private String status; // 状态：自由身、俱乐部成员
    private String joinStatus; // 加入状态：待审核、已审核、拒绝
    private Long applyTeamId; // 申请加入的俱乐部ID
    private String applyReason; // 申请理由
    private String adminRemark; // 管理员审核备注
    private String clubRemark; // 俱乐部审核备注

    // 兼容构建环境：显式提供常用 getter/setter（避免 Lombok 注解处理异常导致编译失败）
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public Double getMarketValue() { return marketValue; }
    public void setMarketValue(Double marketValue) { this.marketValue = marketValue; }
    public Integer getIsFreeAgent() { return isFreeAgent; }
    public void setIsFreeAgent(Integer isFreeAgent) { this.isFreeAgent = isFreeAgent; }
    public String getTransferRecord() { return transferRecord; }
    public void setTransferRecord(String transferRecord) { this.transferRecord = transferRecord; }
    public String getMatchRecord() { return matchRecord; }
    public void setMatchRecord(String matchRecord) { this.matchRecord = matchRecord; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getApplyTeamId() { return applyTeamId; }
    public void setApplyTeamId(Long applyTeamId) { this.applyTeamId = applyTeamId; }
    public String getApplyReason() { return applyReason; }
    public void setApplyReason(String applyReason) { this.applyReason = applyReason; }
}