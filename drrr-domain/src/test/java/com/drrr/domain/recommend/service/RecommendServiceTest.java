package com.drrr.domain.recommend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.drrr.core.code.Gender;
import com.drrr.core.code.TechBlogCode;
import com.drrr.domain.category.entity.Category;
import com.drrr.domain.category.entity.CategoryWeight;
import com.drrr.domain.category.repository.CategoryRepository;
import com.drrr.domain.category.repository.CategoryWeightRepository;
import com.drrr.domain.category.service.RecommendPostService;
import com.drrr.domain.log.entity.post.MemberPostLog;
import com.drrr.domain.log.repository.MemberTechBlogPostRepository;
import com.drrr.domain.member.entity.Member;
import com.drrr.domain.member.entity.MemberRole;
import com.drrr.domain.member.repository.MemberRepository;
import com.drrr.domain.techblogpost.entity.TechBlogPost;
import com.drrr.domain.techblogpost.entity.TechBlogPostCategory;
import com.drrr.domain.techblogpost.repository.TechBlogPostCategoryRepository;
import com.drrr.domain.techblogpost.repository.TechBlogPostRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RecommendServiceTest {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TechBlogPostRepository techBlogPostRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CategoryWeightRepository categoryWeightRepository;
    @Autowired
    private MemberTechBlogPostRepository memberTechBlogPostRepository;

    @Autowired
    private TechBlogPostCategoryRepository techBlogPostCategoryRepository;

    @Autowired
    private RecommendPostService recommendPostService;

    @BeforeEach
    void setup() {
        Member member = Member.builder()
                .email("example@drrr.com")
                .nickname("user1")
                .gender(Gender.MAN)
                .provider("kakao")
                .providerId("12345")
                .imageUrl("http://example.com/image")
                .role(MemberRole.USER)
                .build();
        memberRepository.save(member);

        List<TechBlogPost> techBlogPosts = IntStream.rangeClosed(1, 100).mapToObj(i -> {
            //현재로부터 몇년전까지 랜덤으로 연월일을 뽑을지 정함
            LocalDate createdDate = LocalDate.of(2023, 9, 30);
            createdDate.minusDays(i);
            String author = "Author" + i; // 짝수 인덱스에서만 저자 설정
            String thumbnailUrl = "http://example.com/thumbnail" + i + ".jpg";
            String title = "Title" + i;
            String summary = (i % 3 == 0) ? "Summary" + i : null; // 3의 배수 인덱스에서만 요약 설정
            String urlSuffix = "/suffix/" + i;
            String url = "http://example.com/suffix/" + i;
            TechBlogCode techBlogCode = TechBlogCode.values()[i
                    % TechBlogCode.values().length]; // 순환적으로 TechBlogCode 값 할당
            return TechBlogPost.builder()
                    .createdDate(createdDate)
                    .author(author)
                    .thumbnailUrl(thumbnailUrl)
                    .title(title)
                    .summary(summary)
                    .urlSuffix(urlSuffix)
                    .url(url)
                    .crawlerGroup(TechBlogCode.KAKAO)
                    .build();
        }).collect(Collectors.toList());
        techBlogPostRepository.saveAll(techBlogPosts);

        List<Category> categories = IntStream.rangeClosed(1, 10).mapToObj(i -> {
            String categoryName = "Category" + i;
            String categoryDisplayName = "Display Category" + i;
            return Category.builder()
                    .uniqueName(categoryName)
                    .categoryDisplayName(categoryDisplayName)
                    .build();
        }).collect(Collectors.toList());
        categoryRepository.saveAll(categories);

        /**
         * Member Id 1이 선호하는 카테고리 3, 5, 7
         * Member Id 1이 추가적으로 읽은 카테고리 2, 8
         * Member Id 1의 가중치 값 C2-8, C3-3, C5-4, C7-2, C8-2
         */
        List<Category> categoryWeights = categoryRepository.findByIds(Arrays.asList(2L, 3L, 5L, 7L, 8L)).get();
        List<Double> weights = Arrays.asList(8.0, 3.0, 4.0, 2.0, 2.0);
        List<CategoryWeight> categoryWeightList = new ArrayList<>();
        IntStream.range(0, categoryWeights.size()).forEach(i -> {
            Category category = categoryWeights.get(i);
            double value = weights.get(i);
            boolean preferred = i == 3 || i == 5 || i == 7;

            categoryWeightList.add(new CategoryWeight(member, category, value, preferred));
        });
        categoryWeightRepository.saveAll(categoryWeightList);

        List<TechBlogPost> posts = techBlogPostRepository.findAll();
        List<Category> categoryList = categoryRepository.findAll();
        List<TechBlogPostCategory> techBlogPostCategories = new ArrayList<>();

        /**
         * Member Id 1이 읽은 Post Id 목록 : 1, 3, 5, 7, 9
         * [P1-C3,C5,C7], [P3-C2,C3,C7], [P5-C9], [P7-C4,C6,C9], [P9-C1,C2,C3]
         * 그 외 나머지 Post는 P(2,4,6,8,10)~P(50)-C2,C3,C5,C7,C8를 가지고 있고 P(51)~P(100) C-8로 통일
         */

        List<MemberPostLog> logs = new ArrayList<>();
        logs.add(MemberPostLog.builder()
                .postId(1L)
                .memberId(1L)
                .isRecommended(false)
                .isRead(true)
                .build());
        logs.add(MemberPostLog.builder()
                .postId(3L)
                .memberId(1L)
                .isRecommended(false)
                .isRead(true)
                .build());
        logs.add(MemberPostLog.builder()
                .postId(5L)
                .memberId(1L)
                .isRecommended(false)
                .isRead(true)
                .build());
        logs.add(MemberPostLog.builder()
                .postId(7L)
                .memberId(1L)
                .isRecommended(false)
                .isRead(true)
                .build());
        logs.add(MemberPostLog.builder()
                .postId(9L)
                .memberId(1L)
                .isRecommended(false)
                .isRead(true)
                .build());
        memberTechBlogPostRepository.saveAll(logs);

        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(0))
                .category(categoryList.get(2))
                .build());
        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(0))
                .category(categoryList.get(4))
                .build());
        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(0))
                .category(categoryList.get(6))
                .build());

        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(2))
                .category(categoryList.get(1))
                .build());
        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(2))
                .category(categoryList.get(2))
                .build());
        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(2))
                .category(categoryList.get(6))
                .build());

        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(4))
                .category(categoryList.get(8))
                .build());

        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(6))
                .category(categoryList.get(3))
                .build());
        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(6))
                .category(categoryList.get(5))
                .build());
        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(6))
                .category(categoryList.get(8))
                .build());

        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(8))
                .category(categoryList.get(0))
                .build());
        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(8))
                .category(categoryList.get(1))
                .build());
        techBlogPostCategories.add(TechBlogPostCategory.builder()
                .post(posts.get(8))
                .category(categoryList.get(2))
                .build());

        IntStream.rangeClosed(1, 49).forEach(i -> {
            if (i < 10 && i % 2 == 1) {
                TechBlogPost post = posts.get(i);
                IntStream.rangeClosed(1, 7).forEach(j -> {
                    Category category = categoryList.get(j);
                    techBlogPostCategories.add(TechBlogPostCategory.builder()
                            .post(post)
                            .category(category)
                            .build());
                });
            } else if (i >= 10) {
                TechBlogPost post = posts.get(i);
                Category category = categoryList.get(7);
                techBlogPostCategories.add(TechBlogPostCategory.builder()
                        .post(post)
                        .category(category)
                        .build());
            }
        });

        techBlogPostCategoryRepository.saveAll(techBlogPostCategories);
    }

    @Test
    void 게시물_추천이_정상적으로_작동합니다() {
        List<TechBlogPost> techBlogPosts = recommendPostService.recommendPosts(1L);
        List<Long> postIds = techBlogPosts.stream()
                .map(post -> post.getId()).toList();

        assertThat(postIds).containsExactlyInAnyOrder(2L, 4L, 6L, 8L, 10L);
    }


}
