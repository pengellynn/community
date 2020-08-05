package com.ripon.community.service.impl;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ripon.community.dao.DiscussPostMapper;
import com.ripon.community.entity.DiscussPost;
import com.ripon.community.entity.DiscussPostExample;
import com.ripon.community.service.DiscussPostService;
import com.ripon.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostServiceImpl implements DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    DiscussPostMapper discussPostMapper;
    @Autowired
    SensitiveFilter sensitiveFilter;
    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 3) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int pageNum = Integer.valueOf(params[0]);
                        int pageSize = Integer.valueOf(params[1]);
                        String clause = params[2];

                        logger.debug("load post list from DB.");
                        PageHelper.startPage(pageNum, pageSize);
                        DiscussPostExample example = new DiscussPostExample();
                        example.setOrderByClause(clause);
                        return discussPostMapper.selectByExample(example);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.info("load post rows from DB.");
                        Long discussPostRows = getDiscussPostRows(key);
                        return discussPostRows.intValue();
                    }
                });
    }

    @Override
    public PageInfo<DiscussPost> getDiscussPosts(int pageNum, int pageSize, String clause) {
        PageHelper.startPage(pageNum, pageSize);
        DiscussPostExample example = new DiscussPostExample();
        example.setOrderByClause(clause);
        List<DiscussPost> discussPosts = discussPostMapper.selectByExample(example);
        return new PageInfo<>(discussPosts);
    }

    @Override
    public PageInfo<DiscussPost> getDiscussPosts(int userId, int pageNum, int pageSize, String clause) {
        if (userId == 0) {
            System.out.println("load post List from cache");
            return new PageInfo<>(postListCache.get(pageNum + ":" + pageSize + ":" + clause));
        }

        logger.info("load post list from DB.");
        PageHelper.startPage(pageNum, pageSize);
        DiscussPostExample example = new DiscussPostExample();
        example.setOrderByClause(clause);
        DiscussPostExample.Criteria criteria = example.createCriteria();
        criteria.andUserIdEqualTo(userId);
        List<DiscussPost> discussPosts = discussPostMapper.selectByExample(example);
        return new PageInfo<>(discussPosts);
    }

    @Override
    public Long getDiscussPostRows() {
        logger.debug("load post rows from DB.");
        DiscussPostExample example = new DiscussPostExample();
        return discussPostMapper.countByExample(example);
    }

    @Override
    public Long getDiscussPostRows(int userId) {
        if (userId == 0) {
            return postRowsCache.get(userId).longValue();
        }

        logger.debug("load post rows from DB.");
        DiscussPostExample example = new DiscussPostExample();
        DiscussPostExample.Criteria criteria = example.createCriteria();
        criteria.andUserIdEqualTo(userId);
        return discussPostMapper.countByExample(example);
    }

    @Override
    public void insertDiscussPost(DiscussPost discussPost) {
        if (discussPost == null) {
            throw new IllegalArgumentException("文章不能为空");
        }
        // 转义HTML标记
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        //过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        discussPostMapper.insertSelective(discussPost);
    }

    @Override
    public void updateType(int discussPostId, int type) {
        DiscussPost discussPost = new DiscussPost();
        discussPost.setType(type);
        DiscussPostExample example = new DiscussPostExample();
        DiscussPostExample.Criteria criteria = example.createCriteria();
        criteria.andIdEqualTo(discussPostId);
        discussPostMapper.updateByExampleSelective(discussPost, example);
    }

    @Override
    public void updateScore(int discussPostId, double score) {
        DiscussPost discussPost = new DiscussPost();
        discussPost.setScore(score);
        DiscussPostExample example = new DiscussPostExample();
        DiscussPostExample.Criteria criteria = example.createCriteria();
        criteria.andIdEqualTo(discussPostId);
        discussPostMapper.updateByExampleSelective(discussPost, example);
    }

    @Override
    public void updateStatus(int discussPostId, int status) {
        DiscussPost discussPost = new DiscussPost();
        discussPost.setStatus(status);
        DiscussPostExample example = new DiscussPostExample();
        DiscussPostExample.Criteria criteria = example.createCriteria();
        criteria.andIdEqualTo(discussPostId);
        discussPostMapper.updateByExampleSelective(discussPost, example);
    }

    @Override
    public DiscussPost getDiscussPost(int discussPostId) {
        return discussPostMapper.selectByPrimaryKey(discussPostId);
    }
}
