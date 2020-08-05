package com.ripon.community.service;

import com.github.pagehelper.PageInfo;
import com.ripon.community.entity.Comment;

public interface CommentService {
    PageInfo<Comment> getCommentsByEntity(int entityType, int entityId, int pageNum, int pageSize);
    Long getCommentCount(int entityType, int entityId);
    void insertComment(Comment comment);
    Comment getComment(int id);
}
