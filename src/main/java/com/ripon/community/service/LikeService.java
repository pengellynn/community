package com.ripon.community.service;

public interface LikeService {
    void like(int userId, int entityType, int entityId, int entityUserId);
    long getEntityLikeCount(int entityType, int entityId);
    int getEntityLikeStatus(int userId, int EntityType, int entityId);
    int getUserLikeCount(int userId);
}
