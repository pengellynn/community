package com.ripon.community;

import com.ripon.community.dao.CommentMapper;
import com.ripon.community.dao.DiscussPostMapper;
import com.ripon.community.dao.elasticsearch.DiscussPostRepository;
import com.ripon.community.entity.DiscussPost;
import com.ripon.community.service.DiscussPostService;
import com.ripon.community.util.SensitiveFilter;
import kafka.Kafka;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CommunityApplicationTests {
	@Autowired
	DiscussPostService discussPostService;
	@Autowired
	private DiscussPostRepository discussRepository;

	@Autowired
	private ElasticsearchTemplate elasticTemplate;
	@Autowired
	KafkaTemplate kafkaTemplate;
	@Test
	public void contextLoads() {

	}
	@Test
	public void kafkaSendTest() {
		kafkaTemplate.send("hello", "hello kafka");
	}
	@Test
	public void testInsert() {
		discussRepository.save(discussPostService.getDiscussPost(241));
		discussRepository.save(discussPostService.getDiscussPost(242));
		discussRepository.save(discussPostService.getDiscussPost(243));
	}

	@Test
	public void testInsertList() {
		discussRepository.saveAll(discussPostService.getDiscussPosts(1, 100, "`type` desc, `create_time` desc").getList());
	}
	@Test
	public void testSearchByTemplate() {
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
				.withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
				.withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
				.withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
				.withPageable(PageRequest.of(0, 10))
				.withHighlightFields(
						new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
						new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
				).build();

		Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
			@Override
			public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
				SearchHits hits = response.getHits();
				if (hits.getTotalHits() <= 0) {
					return null;
				}

				List<DiscussPost> list = new ArrayList<>();
				for (SearchHit hit : hits) {
					DiscussPost post = new DiscussPost();

					String id = hit.getSourceAsMap().get("id").toString();
					post.setId(Integer.valueOf(id));

					String userId = hit.getSourceAsMap().get("userId").toString();
					post.setUserId(Integer.valueOf(userId));

					String title = hit.getSourceAsMap().get("title").toString();
					post.setTitle(title);

					String content = hit.getSourceAsMap().get("content").toString();
					post.setContent(content);

					String status = hit.getSourceAsMap().get("status").toString();
					post.setStatus(Integer.valueOf(status));

					String createTime = hit.getSourceAsMap().get("createTime").toString();
					post.setCreateTime(new Date(Long.valueOf(createTime)));

					String commentCount = hit.getSourceAsMap().get("commentCount").toString();
					post.setCommentCount(Integer.valueOf(commentCount));

					// 处理高亮显示的结果
					HighlightField titleField = hit.getHighlightFields().get("title");
					if (titleField != null) {
						post.setTitle(titleField.getFragments()[0].toString());
					}

					HighlightField contentField = hit.getHighlightFields().get("content");
					if (contentField != null) {
						post.setContent(contentField.getFragments()[0].toString());
					}

					list.add(post);
				}

				return new AggregatedPageImpl(list, pageable,
						hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore());
			}
		});

		System.out.println(page.getTotalElements());
		System.out.println(page.getTotalPages());
		System.out.println(page.getNumber());
		System.out.println(page.getSize());
		for (DiscussPost post : page) {
			System.out.println(post);
		}
	}

}
