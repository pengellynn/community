package com.ripon.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.Message;
import com.ripon.community.entity.User;
import com.ripon.community.service.MessageService;
import com.ripon.community.service.UserService;
import com.ripon.community.util.HostHolder;
import com.ripon.community.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController {
    @Autowired
    MessageService messageService;
    @Autowired
    HostHolder hostHolder;
    @Autowired
    UserService userService;

    @GetMapping("/letter/list")
    public String getLetterList(@RequestParam(defaultValue = "1",required = false) Integer pageNum,
                                @RequestParam(defaultValue = "5", required = false) Integer pageSize,
                                Model model) {
        User user = hostHolder.getUser();
        Integer userId = user.getId();
        PageInfo<Message> pageInfo = messageService.getConversations(userId, pageNum, pageSize);
        List<Message> conversationList = pageInfo.getList();
        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Message message: conversationList) {
            Map<String, Object> map = new HashMap<>();
            map.put("conversation", message);
            String conversationId = message.getConversationId();
            int messageCount = messageService.getMessageCount(conversationId);
            map.put("messageCount", messageCount);
            int unreadMessageCount = messageService.getUnreadMessageCount(userId,conversationId);
            map.put("unreadMessageCount", unreadMessageCount);
            int targetId = userId == message.getFromId()? message.getToId():message.getFromId();
            map.put("target", userService.getUserById(targetId));
            conversations.add(map);
        }
        model.addAttribute("conversations", conversations);
        int unreadMessageTotalCount = messageService.getUnreadMessageCount(userId, null);
        model.addAttribute("unreadMessageTotalCount", unreadMessageTotalCount);
        int noticeUnreadCount = messageService.getUnreadNoticeCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);
        model.addAttribute("totalPage",pageInfo.getPages());
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("path", "/letter/list");
        return "/site/letter";
    }

    @GetMapping("/letter/detail/{conversationId}")
    public String getLetterDetail(@PathVariable("conversationId") String conversationId,
                                  @RequestParam(defaultValue = "1",required = false) Integer pageNum,
                                  @RequestParam(defaultValue = "5", required = false) Integer pageSize,
                                  Model model) {
        PageInfo<Message> pageInfo = messageService.getMessages(conversationId, pageNum, pageSize);
        List<Message> messageList = pageInfo.getList();
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Message message : messageList) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", message);
            User fromUser = userService.getUserById(message.getFromId());
            map.put("fromUser", fromUser);
            messages.add(map);
        }
        model.addAttribute("messages", messages);
        model.addAttribute("fromUser", getFromUser(conversationId));
        List<Integer> ids = getMessageIds(messageList);
        if (ids.size() != 0){
            messageService.readMessage(ids);
        }
        model.addAttribute("totalPage",pageInfo.getPages());
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("path", "/letter/detail/"+conversationId);
        return "/site/letter-detail";
    }

    private List<Integer> getMessageIds(List<Message> messageList) {
        if (messageList == null) {
            return null;
        }
        User user = hostHolder.getUser();
        List<Integer> ids = new ArrayList<>();
        for (Message message: messageList) {
            if (message.getToId() == user.getId() && message.getStatus() == 0) {
                ids.add(message.getId());
            }
        }
        return ids;
    }

    private User getFromUser(String conversationId) {
        String[] ids = conversationId.split("_");
        Integer id0 = Integer.parseInt(ids[0]);
        Integer id1 = Integer.parseInt(ids[1]);
        Integer id = hostHolder.getUser().getId() == id0? id1: id0;
        return userService.getUserById(id);
    }

    @ResponseBody
    @PostMapping("/letter/send")
    public String sendLetter(String toName, String content) {
        User toUser = userService.getUserByName(toName);
        if (toUser == null) {
            return JsonUtils.getJSONString(1, "用户不存在");
        }
        Message message = new Message();
        Integer fromId = hostHolder.getUser().getId();
        Integer toId = toUser.getId();
        message.setFromId(fromId);
        message.setToId(toId);
        String conversationId = null;
        if (fromId < toId) {
             conversationId = fromId + "_" + toId;
        } else {
            conversationId = toId + "_" + fromId;
        }
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setStatus(0);
        message.setCreateTime(new Date());
        messageService.insertMessage(message);
        return JsonUtils.getJSONString(0);
    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        User user = hostHolder.getUser();

        // 查询评论类通知
        Message message = messageService.getLatestNotice(user.getId(), CommunityConstant.TOPIC_COMMENT);
        Map<String, Object> messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userService.getUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.getNoticeCount(user.getId(), CommunityConstant.TOPIC_COMMENT);
            messageVO.put("count", count);

            int unread = messageService.getUnreadNoticeCount(user.getId(), CommunityConstant.TOPIC_COMMENT);
            messageVO.put("unread", unread);
        }
        model.addAttribute("commentNotice", messageVO);

        // 查询点赞类通知
        message = messageService.getLatestNotice(user.getId(), CommunityConstant.TOPIC_LIKE);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userService.getUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));
            messageVO.put("postId", data.get("postId"));

            int count = messageService.getNoticeCount(user.getId(),  CommunityConstant.TOPIC_LIKE);
            messageVO.put("count", count);

            int unread = messageService.getUnreadNoticeCount(user.getId(),  CommunityConstant.TOPIC_LIKE);
            messageVO.put("unread", unread);
        }
        model.addAttribute("likeNotice", messageVO);

        // 查询关注类通知
        message = messageService.getLatestNotice(user.getId(), CommunityConstant.TOPIC_FOLLOW);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVO.put("user", userService.getUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));
            messageVO.put("entityId", data.get("entityId"));

            int count = messageService.getNoticeCount(user.getId(), CommunityConstant.TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unread = messageService.getUnreadNoticeCount(user.getId(), CommunityConstant.TOPIC_FOLLOW);
            messageVO.put("unread", unread);
        }
        model.addAttribute("followNotice", messageVO);

        // 查询未读消息数量
        int letterUnreadCount = messageService.getUnreadMessageCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        int noticeUnreadCount = messageService.getUnreadNoticeCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic,
                                  @RequestParam(defaultValue = "1", required = false) Integer pageNum,
                                  @RequestParam(defaultValue = "5", required = false) Integer pageSize,
                                  Model model) {
        User user = hostHolder.getUser();
        PageInfo<Message> pageInfo = messageService.getNotices(user.getId(), topic, pageNum, pageSize);
        List<Message> noticeList = pageInfo.getList();
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                // 通知
                map.put("notice", notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.getUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 通知作者
                map.put("fromUser", userService.getUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPage", pageInfo.getPages());
        model.addAttribute("path", "/notice/detail/" + topic);
        // 设置已读
        List<Integer> ids = getMessageIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }
}
