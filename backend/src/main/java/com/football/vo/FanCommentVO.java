package com.football.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class FanCommentVO {
    private Long id;
    private Long matchId;
    private String matchTitle;

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;

    private String content;
    private Integer likes;
    private Date createTime;

    private List<FanCommentReplyVO> replies;

    // 当前评论下的回复总数（用于按钮展示）
    private Integer replyCount;

    // 当前登录用户（reply_to_user_id）相关回复数，用于“你被回复”提示
    private Integer myReplyCount;
}

