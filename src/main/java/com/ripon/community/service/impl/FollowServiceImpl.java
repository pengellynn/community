package com.ripon.community.service.impl;

import com.ripon.community.constant.CommunityConstant;
import com.ripon.community.entity.User;
import com.ripon.community.service.FollowService;
import com.ripon.community.service.UserService;
import com.ripon.community.util.RedisKeyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowServiceImpl implements FollowService {
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    UserService userService;
    @Override
    public void follow(int entityType, int entityId, int userId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followingKey = RedisKeyUtils.getFollowingKey(userId, entityType);
                String followerKey = RedisKeyUtils.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().add(followingKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                return operations.exec();
            }
        });
    }

    @Override
    public void unFollow(int entityType, int entityId, int userId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followingKey = RedisKeyUtils.getFollowingKey(userId, entityType);
                String followerKey = RedisKeyUtils.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().remove(followingKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);
                return operations.exec();
            }
        });
    }

    // 获取某实体粉丝数量
    @Override
    public long getFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtils.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }
    // 获取某用户关注的某实体数量
    @Override
    public long getFollowingCount(int userId, int entityType) {
        String followingKey = RedisKeyUtils.getFollowingKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followingKey);
    }

    // 获取某用户的粉丝列表
    @Override
    public List<Map<String, Object>> getFollowers(int userId, int pageNum, int pageSize) {
        String followerKey = RedisKeyUtils.getFollowerKey(CommunityConstant.ENTITY_TYPE_USER, userId);
        return getFollowList(followerKey, pageNum, pageSize);
    }

    // 获取某用户关注的人列表
    @Override
    public List<Map<String, Object>> getFollowings(int userId, int pageNum, int pageSize) {
        String followingKey = RedisKeyUtils.getFollowingKey(userId, CommunityConstant.ENTITY_TYPE_USER);
        return getFollowList(followingKey, pageNum, pageSize);
    }

    private List<Map<String, Object>> getFollowList(String key, int pageNum, int pageSize) {
        long begin = (pageNum -1)*pageSize;
        long end = begin + pageSize - 1;
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(key, begin, end);
        if (targetIds == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.getUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(key, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

    // 查询当前用户是否已关注该实体
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtils.getFollowingKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }
}
