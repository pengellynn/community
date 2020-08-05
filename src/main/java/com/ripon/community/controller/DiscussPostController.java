package com.ripon.community.controller;

import com.github.pagehelper.PageInfo;
import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.Comment;
import com.ripon.community.entity.DiscussPost;
import com.ripon.community.entity.Event;
import com.ripon.community.entity.User;
import com.ripon.community.mq.EventProducer;
import com.ripon.community.service.CommentService;
import com.ripon.community.service.DiscussPostService;
import com.ripon.community.service.LikeService;
import com.ripon.community.service.UserService;
import com.ripon.community.util.HostHolder;
import com.ripon.community.util.JsonUtils;
import com.ripon.community.util.RedisKeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class DiscussPostController {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    HostHolder hostHolder;
    @Autowired
    DiscussPostService discussPostService;
    @Autowired
    UserService userService;
    @Autowired
    CommentService commentService;
    @Autowired
    LikeService likeService;
    @Autowired
    EventProducer eventProducer;
    @Autowired
    RedisTemplate redisTemplate;


    @PostMapping("/discussPost")
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        User user = hostHolder.getUser();
        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setUserId(user.getId());
        discussPost.setCreateTime(new Date());
        discussPost.setType(0);
        discussPost.setStatus(0);
        discussPost.setCommentCount(0);
        discussPost.setScore(0.0);
        discussPostService.insertDiscussPost(discussPost);
        // 触发发帖事件
        sendPublishMessage(discussPost.getId());
        // 计算帖子分数
        String redisKey = RedisKeyUtils.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPost.getId());
        return JsonUtils.getJSONString(200, "发布成功");
    }

    @GetMapping("/discussPost/detail/{id}")
    public String getDiscussPostDetail(@PathVariable("id") Integer discussPostId,
                                       @RequestParam(defaultValue = "1", required = false) Integer pageNum,
                                       @RequestParam(defaultValue = "5", required = false) Integer pageSize,
                                       Model model) {
        // 帖子
        DiscussPost discussPost = discussPostService.getDiscussPost(discussPostId);
        model.addAttribute("post", discussPost);
        // 作者
        User postUser = userService.getUserById(discussPost.getUserId());
        model.addAttribute("postUser", postUser);
        // 当前用户
        User user = hostHolder.getUser();
        long likeCount = likeService.getEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        int likeStatus = user == null ? 0 : likeService.getEntityLikeStatus(user.getId(), CommunityConstant.ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);
        // 分页的评论
        PageInfo<Comment> pageInfo = commentService.getCommentsByEntity(CommunityConstant.ENTITY_TYPE_POST, discussPostId, pageNum, pageSize);
        List<Comment> commentList = pageInfo.getList();
        // 评论Vo列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Integer commentId = comment.getId();
                Map<String, Object> commentVo = new HashMap<>();
                // 评论
                commentVo.put("comment", comment);
                // 评论人
                commentVo.put("user", userService.getUserById(comment.getUserId()));
                // 该评论的赞数量
                likeCount = likeService.getEntityLikeCount(CommunityConstant.ENTITY_TYPE_COMMENT, commentId);
                commentVo.put("likeCount", likeCount);
                // 当前用户对该评论的赞状态
                likeStatus = user == null ? 0 : likeService.getEntityLikeStatus(user.getId(), CommunityConstant.ENTITY_TYPE_COMMENT, commentId);
                commentVo.put("likeStatus", likeStatus);
                // 回复
                PageInfo<Comment> page = commentService.getCommentsByEntity(CommunityConstant.ENTITY_TYPE_COMMENT, commentId, 1, 1000);
                List<Comment> replytList = page.getList();
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                for (Comment reply : replytList) {
                    Integer replyId = reply.getId();
                    Map<String, Object> replyVo = new HashMap<>();
                    // 回复
                    replyVo.put("reply", reply);
                    // 回复人
                    replyVo.put("user", userService.getUserById(reply.getUserId()));
                    // 赞的数量
                    likeCount = likeService.getEntityLikeCount(CommunityConstant.ENTITY_TYPE_COMMENT, replyId);
                    replyVo.put("likeCount", likeCount);
                    // 当前用户对该回复的赞状态
                    likeStatus = user == null ? 0 : likeService.getEntityLikeStatus(user.getId(), CommunityConstant.ENTITY_TYPE_COMMENT, replyId);
                    replyVo.put("likeStatus", likeStatus);
                    // 回复目标
                    User target = reply.getTargetId() == 0 ? null : userService.getUserById(reply.getTargetId());
                    replyVo.put("target", target);
                    replyVoList.add(replyVo);
                }
                commentVo.put("replyVoList", replyVoList);
                // 回复数量
                Long replyCount = commentService.getCommentCount(CommunityConstant.ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("commentVoList", commentVoList);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPage", pageInfo.getPages());
        model.addAttribute("path", "/discussPost/detail/" + discussPostId);
        return "/site/discuss-detail";
    }

    // 置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id) {
        discussPostService.updateType(id, 1);
        // 触发发帖事件
        sendPublishMessage(id);
        return JsonUtils.getJSONString(0);
    }

    // 加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id) {
        discussPostService.updateStatus(id, 1);
        // 触发发帖事件
        sendPublishMessage(id);
        // 计算帖子分数
        String redisKey = RedisKeyUtils.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return JsonUtils.getJSONString(0);
    }

    // 删除
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);
        // 触发发帖事件
        sendPublishMessage(id);
        return JsonUtils.getJSONString(0);
    }

    private void sendPublishMessage(int entityId) {
        Event event = new Event();
        event.setTopic(CommunityConstant.TOPIC_PUBLISH);
        event.setUserId(hostHolder.getUser().getId());
        event.setEntityType(CommunityConstant.ENTITY_TYPE_POST);
        event.setEntityId(entityId);
        eventProducer.fireEvent(event);
    }
}
