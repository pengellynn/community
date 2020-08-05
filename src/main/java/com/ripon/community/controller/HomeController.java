package com.ripon.community.controller;

import com.github.pagehelper.PageInfo;
import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.DiscussPost;
import com.ripon.community.entity.User;
import com.ripon.community.service.DiscussPostService;
import com.ripon.community.service.LikeService;
import com.ripon.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    DiscussPostService discussPostService;
    @Autowired
    UserService userService;
    @Autowired
    LikeService likeService;

    @GetMapping("/index")
    public String getIndexPage(Model model,
                               @RequestParam(defaultValue = "1", required = false) Integer pageNum,
                               @RequestParam(defaultValue = "10", required = false) Integer pageSize){
        String clause = "`type` desc, `create_time` desc";
//        PageInfo<DiscussPost> pageInfo = discussPostService.getDiscussPosts(pageNum,pageSize,clause);
        PageInfo<DiscussPost> pageInfo = discussPostService.getDiscussPosts(0,pageNum,pageSize,clause);
        List<DiscussPost> list = pageInfo.getList();
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        for (DiscussPost discussPost: list){
            Map<String, Object> map = new HashMap<>();
            map.put("post", discussPost);
            User user = userService.getUserById(discussPost.getUserId());
            map.put("user", user);
            long likeCount = likeService.getEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST, discussPost.getId());
            map.put("likeCount", likeCount);
            discussPosts.add(map);
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("totalPage",pageInfo.getPages());
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("path", "/index");
        return "/index";
    }


}
