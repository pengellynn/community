package com.ripon.community.controller;

import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.Comment;
import com.ripon.community.entity.DiscussPost;
import com.ripon.community.entity.Event;
import com.ripon.community.mq.EventProducer;
import com.ripon.community.service.CommentService;
import com.ripon.community.service.DiscussPostService;
import com.ripon.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Date;

@Controller
public class CommentController {
    @Autowired
    CommentService commentService;

    @Autowired
    HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @PostMapping("/comment/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") Integer discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.insertComment(comment);

        Event event = new Event();
        event.setTopic(CommunityConstant.TOPIC_COMMENT);
        event.setUserId(comment.getUserId());
        event.setEntityType(comment.getEntityType());
        event.setEntityId(comment.getEntityId());
        event.setData("postId", discussPostId);
        if (comment.getEntityType() == CommunityConstant.ENTITY_TYPE_POST) {
            DiscussPost discussPost = discussPostService.getDiscussPost(comment.getEntityId());
            event.setEntityUserId(discussPost.getUserId());
        } else if (comment.getEntityType() == CommunityConstant.ENTITY_TYPE_COMMENT) {
            Comment target = commentService.getComment(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);
        if (comment.getEntityType() == CommunityConstant.ENTITY_TYPE_POST) {
            // 触发发帖事件
            event = new Event();
            event.setTopic(CommunityConstant.TOPIC_PUBLISH);
            event.setUserId(comment.getUserId());
            event.setEntityType(CommunityConstant.ENTITY_TYPE_POST);
            event.setEntityId(discussPostId);
            eventProducer.fireEvent(event);
        }
        return "redirect:/discussPost/detail/" + discussPostId;
    }
}
