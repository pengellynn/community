package com.ripon.community.controller;

import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.Event;
import com.ripon.community.entity.User;
import com.ripon.community.mq.EventProducer;
import com.ripon.community.service.LikeService;
import com.ripon.community.util.HostHolder;
import com.ripon.community.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

@Controller
public class LikeController {
    @Autowired
    LikeService likeService;
    @Autowired
    HostHolder hostHolder;
    @Autowired
    EventProducer eventProducer;

    @ResponseBody
    @PostMapping("/like")
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        int userId = hostHolder.getUser().getId();
        likeService.like(userId, entityType, entityId, entityUserId);
        long likeCount = likeService.getEntityLikeCount(entityType, entityId);
        int likeStatus = likeService.getEntityLikeStatus(userId, entityType, entityId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("likeCount",likeCount);
        map.put("likeStatus", likeStatus);
        if (likeStatus == 1) {
            Event event = new Event();
            event.setTopic(CommunityConstant.TOPIC_LIKE);
            event.setUserId(userId);
            event.setEntityType(entityType);
            event.setEntityId(entityId);
            event.setEntityUserId(entityUserId);
            event.setData("postId", postId);
            eventProducer.fireEvent(event);
        }
        return JsonUtils.getJSONString(0, null, map);
    }
}
