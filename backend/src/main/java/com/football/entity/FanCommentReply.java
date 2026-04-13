package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 球迷评论的回复（支持对“评论”回复，也支持对“回复”继续回复）
 * - commentId：所属顶级评论 fan_comment.id
 * - parentReplyId：被回复的回复 id（顶级回复为 null）
 * - replyToUserId：被回复的用户 id（用于 UI @提醒）
 * - replyUserId：当前回复的用户 id
 */
@Data
@TableName("fan_comment_reply")
public class FanCommentReply {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long commentId;

    private Long parentReplyId;

    private Long replyToUserId;

    private Long replyUserId;

    private String content;

    private Date createTime;
}

