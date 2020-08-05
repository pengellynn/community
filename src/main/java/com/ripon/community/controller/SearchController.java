package com.ripon.community.controller;

import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.DiscussPost;
import com.ripon.community.service.ElasticsearchService;
import com.ripon.community.service.LikeService;
import com.ripon.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword,
                         @RequestParam(defaultValue = "1", required = false) Integer pageNum,
                         @RequestParam(defaultValue = "10", required = false) Integer pageSize,
                         Model model) {
        // 搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(keyword, pageNum-1, pageSize);
        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", post);
                // 作者
                map.put("user", userService.getUserById(post.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.getEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST, post.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalPage",searchResult == null? 0:searchResult.getTotalPages());
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("path", "/search?"+keyword);

        return "/site/search";
    }

}
