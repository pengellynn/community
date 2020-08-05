package com.ripon.community.controller;

import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.Event;
import com.ripon.community.entity.User;
import com.ripon.community.mq.EventProducer;
import com.ripon.community.service.FollowService;
import com.ripon.community.service.UserService;
import com.ripon.community.util.HostHolder;
import com.ripon.community.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController {

    @Autowired
    FollowService followService;

    @Autowired
    HostHolder hostHolder;

    @Autowired
    UserService userService;

    @Autowired
    EventProducer eventProducer;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        Integer userId = hostHolder.getUser().getId();

        followService.follow(entityType, entityId, userId);
        // 触发关注事件
        Event event = new Event();
        event.setTopic(CommunityConstant.TOPIC_FOLLOW);
        event.setUserId(userId);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return JsonUtils.getJSONString(0, "已关注!");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unFollow(int entityType, int entityId) {
        User user = hostHolder.getUser();

        followService.unFollow(entityType, entityId, user.getId());

        return JsonUtils.getJSONString(0, "已取消关注!");
    }

    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowings(@PathVariable("userId") int userId,
                                @RequestParam(defaultValue = "1", required = false) Integer pageNum,
                                @RequestParam(defaultValue = "5", required = false) Integer pageSize,
                                Model model) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);
        Long followingCount = followService.getFollowingCount(userId, CommunityConstant.ENTITY_TYPE_USER);
        int total = followingCount.intValue();
        int totalPage = total / pageSize == 0 ? total / pageSize : total / pageSize + 1;
        List<Map<String, Object>> userList = followService.getFollowings(userId, pageNum, pageSize);
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("path", "/followees/" + userId);
        return "/site/followee";
    }

    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId,
                               @RequestParam(defaultValue = "1", required = false) Integer pageNum,
                               @RequestParam(defaultValue = "5", required = false) Integer pageSize,
                               Model model) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        model.addAttribute("user", user);
        Long followerCount = followService.getFollowerCount(userId, CommunityConstant.ENTITY_TYPE_USER);
        int total = followerCount.intValue();
        int totalPage = total / pageSize == 0 ? total / pageSize : total / pageSize + 1;
        List<Map<String, Object>> userList = followService.getFollowers(userId, pageNum, pageSize);
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("path", "/followers/" + userId);

        return "/site/follower";
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), CommunityConstant.ENTITY_TYPE_USER, userId);
    }

}
