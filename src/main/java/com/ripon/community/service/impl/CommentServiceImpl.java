package com.ripon.community.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.dao.CommentMapper;
import com.ripon.community.dao.DiscussPostMapper;
import com.ripon.community.entity.Comment;
import com.ripon.community.entity.CommentExample;
import com.ripon.community.entity.DiscussPost;
import com.ripon.community.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {
    @Autowired
    CommentMapper commentMapper;
    @Autowired
    DiscussPostMapper discussPostMapper;
    @Override
    public PageInfo<Comment> getCommentsByEntity(int entityType, int entityId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        CommentExample example = new CommentExample();
        CommentExample.Criteria criteria = example.createCriteria();
        criteria.andEntityTypeEqualTo(entityType);
        criteria.andEntityIdEqualTo(entityId);
        List<Comment> comments = commentMapper.selectByExampleWithBLOBs(example);
        return new PageInfo<>(comments);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Override
    public void insertComment(Comment comment) {
        commentMapper.insertSelective(comment);
        if (comment.getEntityType() == CommunityConstant.ENTITY_TYPE_POST) {
            DiscussPost discussPost = new DiscussPost();
            discussPost.setId(comment.getEntityId());
            CommentExample example = new CommentExample();
            CommentExample.Criteria criteria = example.createCriteria();
            criteria.andEntityTypeEqualTo(comment.getEntityType());
            criteria.andEntityIdEqualTo(comment.getEntityId());
            Long count = commentMapper.countByExample(example);
            discussPost.setCommentCount(count.intValue());
            discussPostMapper.updateByPrimaryKeySelective(discussPost);
        }
    }

    @Override
    public Long getCommentCount(int entityType, int entityId) {
        CommentExample example = new CommentExample();
        CommentExample.Criteria criteria = example.createCriteria();
        criteria.andEntityTypeEqualTo(entityType);
        criteria.andEntityIdEqualTo(entityId);
        return commentMapper.countByExample(example);
    }

    @Override
    public Comment getComment(int id) {
        return commentMapper.selectByPrimaryKey(id);
    }
}
