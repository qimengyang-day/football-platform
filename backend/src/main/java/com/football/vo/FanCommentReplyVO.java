package com.football.vo;

import lombok.Data;

import java.util.Date;

@Data
public class FanCommentReplyVO {
    private Long id;

    // 顶级评论所属 id
    private Long commentId;

    // 被回复的回复 id（顶级回复为 null）
    private Long parentReplyId;

    // 被回复的用户 id（用于 UI @提醒）
    private Long replyToUserId;

    private String replyToUsername;

    // 当前回复用户 id/用户名
    private Long replyUserId;
    private String replyUsername;

    private String username;
    private String content;
    private Date createTime;
}

