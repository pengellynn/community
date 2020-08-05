package com.ripon.community.service;

import com.github.pagehelper.PageInfo;
import com.ripon.community.entity.Message;

import java.util.List;

public interface MessageService {
    PageInfo<Message> getConversations(int userId, int pageNum, int pageSize);
    int getConversationCount(int userId);
    PageInfo<Message> getMessages(String conversationId, int pageNum, int pageSize);
    int getMessageCount(String conversationId);
    int getUnreadMessageCount(int userId, String conversationId);
    Message getMessage(int messageId);
    int insertMessage(Message message);
    int readMessage(List<Integer> ids);
    Message getLatestNotice(int userId, String topic);
    int getNoticeCount(int userId, String topic);
    int getUnreadNoticeCount(int userId, String topic);
    PageInfo<Message> getNotices(int userId, String topic, int pageNum, int pageSize);
}
