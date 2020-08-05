package com.ripon.community.service;

import java.util.List;
import java.util.Map;

public interface FollowService {
    void follow(int entityType, int entityId, int userId);
    void unFollow(int entityType, int entityId, int userId);
    long getFollowerCount(int entityType, int entityId);
    long getFollowingCount(int userId, int entityType);
    List<Map<String, Object>> getFollowers(int userId, int pageNum, int pageSize);
    List<Map<String, Object>> getFollowings(int userId, int pageNum, int pageSize);
    boolean hasFollowed(int userId, int entityType, int entityId);
}
