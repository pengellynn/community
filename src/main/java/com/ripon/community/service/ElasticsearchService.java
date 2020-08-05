package com.ripon.community.service;

import com.ripon.community.entity.DiscussPost;
import org.springframework.data.domain.Page;

public interface ElasticsearchService {
    void saveDiscussPost(DiscussPost post);
    void deleteDiscussPost(int id);
    Page<DiscussPost> searchDiscussPost(String keyword, int pageNum, int pageSize);
}
