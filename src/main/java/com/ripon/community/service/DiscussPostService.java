package com.ripon.community.service;

import com.github.pagehelper.PageInfo;
import com.ripon.community.entity.DiscussPost;

public interface DiscussPostService {
    PageInfo<DiscussPost> getDiscussPosts(int pageNum, int pageSize, String clause);
    PageInfo<DiscussPost> getDiscussPosts(int userId, int pageNum, int pageSize, String clause);
    Long getDiscussPostRows();
    Long getDiscussPostRows(int userId);
    void insertDiscussPost(DiscussPost discussPost);
    DiscussPost getDiscussPost(int discussPostId);
    void updateType(int discussPostId, int type);
    void updateStatus(int discussPostId, int status);
    void updateScore(int discussPostId, double score);
}
