package com.ripon.community.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.dao.MessageMapper;
import com.ripon.community.entity.Message;
import com.ripon.community.entity.MessageExample;
import com.ripon.community.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    MessageMapper messageMapper;
    @Override
    public PageInfo<Message> getConversations(int userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Message> messages = messageMapper.selectConversations(userId);
        return new PageInfo<>(messages);
    }

    @Override
    public int getConversationCount(int userId) {
        return messageMapper.selectConversationCount(userId);
    }

    @Override
    public PageInfo<Message> getMessages(String conversationId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        MessageExample example = new MessageExample();
        example.setOrderByClause("`create_time` desc");
        MessageExample.Criteria criteria = example.createCriteria();
        criteria.andConversationIdEqualTo(conversationId);
        criteria.andStatusNotEqualTo(2);
        criteria.andFromIdNotEqualTo(1);
        List<Message> messages = messageMapper.selectByExampleWithBLOBs(example);
        return new PageInfo<>(messages);
    }

    @Override
    public int getMessageCount(String conversationId) {
        MessageExample example = new MessageExample();
        MessageExample.Criteria criteria = example.createCriteria();
        criteria.andConversationIdEqualTo(conversationId);
        criteria.andStatusNotEqualTo(2);
        criteria.andFromIdNotEqualTo(1);
        Long count = messageMapper.countByExample(example);
        return count.intValue();
    }

    @Override
    public int getUnreadMessageCount(int userId, String conversationId) {
        MessageExample example = new MessageExample();
        MessageExample.Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo(0);
        criteria.andFromIdNotEqualTo(1);
        criteria.andToIdEqualTo(userId);
        if (conversationId !=null) {
            criteria.andConversationIdEqualTo(conversationId);
        }
        Long count = messageMapper.countByExample(example);
        return count.intValue();
    }

    @Override
    public Message getMessage(int messageId) {
        return messageMapper.selectByPrimaryKey(messageId);
    }

    @Override
    public int insertMessage(Message message) {
        return messageMapper.insertSelective(message);
    }

    @Override
    public int readMessage(List<Integer> ids) {
        Message message = new Message();
        message.setStatus(1);
        MessageExample example = new MessageExample();
        MessageExample.Criteria criteria = example.createCriteria();
        criteria.andIdIn(ids);
        return messageMapper.updateByExampleSelective(message, example);
    }

    @Override
    public Message getLatestNotice(int userId, String topic) {
        MessageExample example = new MessageExample();
        example.setOrderByClause("`create_time` desc");
        MessageExample.Criteria criteria = example.createCriteria();
        criteria.andFromIdEqualTo(CommunityConstant.SYSTEM_USER_ID);
        criteria.andToIdEqualTo(userId);
        criteria.andConversationIdEqualTo(topic);
        criteria.andStatusNotEqualTo(2);
        List<Message> messages = messageMapper.selectByExampleWithBLOBs(example);
        if (messages.size() != 0) {
            return messages.get(0);
        }
        return null;
    }

    @Override
    public int getNoticeCount(int userId, String topic) {
        MessageExample example = new MessageExample();
        MessageExample.Criteria criteria = example.createCriteria();
        criteria.andFromIdEqualTo(CommunityConstant.SYSTEM_USER_ID);
        criteria.andToIdEqualTo(userId);
        criteria.andConversationIdEqualTo(topic);
        criteria.andStatusNotEqualTo(2);
        Long count = messageMapper.countByExample(example);
        return count == 0 ? 0 : count.intValue();
    }

    @Override
    public int getUnreadNoticeCount(int userId, String topic) {
        MessageExample example = new MessageExample();
        MessageExample.Criteria criteria = example.createCriteria();
        criteria.andFromIdEqualTo(CommunityConstant.SYSTEM_USER_ID);
        criteria.andToIdEqualTo(userId);
        if (topic != null) {
            criteria.andConversationIdEqualTo(topic);
        }
        criteria.andStatusEqualTo(0);
        Long count = messageMapper.countByExample(example);
        return count == 0 ? 0 : count.intValue();
    }

    @Override
    public PageInfo<Message> getNotices(int userId, String topic, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        MessageExample example = new MessageExample();
        MessageExample.Criteria criteria = example.createCriteria();
        criteria.andFromIdEqualTo(CommunityConstant.SYSTEM_USER_ID);
        criteria.andToIdEqualTo(userId);
        criteria.andConversationIdEqualTo(topic);
        criteria.andStatusNotEqualTo(2);
        List<Message> messages = messageMapper.selectByExampleWithBLOBs(example);
        return new PageInfo<>(messages);
    }
}
