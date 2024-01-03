package com.drrr.domain.category.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.drrr.core.code.techblog.TechBlogCode;
import com.drrr.core.recommandation.constant.constant.HoursConstants;
import com.drrr.core.recommandation.constant.constant.WeightConstants;
import com.drrr.domain.category.entity.Category;
import com.drrr.domain.category.entity.CategoryWeight;
import com.drrr.domain.category.repository.CategoryRepository;
import com.drrr.domain.category.repository.CategoryWeightRepository;
import com.drrr.domain.jpa.config.JpaConfiguration;
import com.drrr.domain.jpa.config.QueryDSLConfiguration;
import com.drrr.domain.member.entity.Member;
import com.drrr.domain.member.repository.MemberRepository;
import com.drrr.domain.techblogpost.entity.TechBlogPost;
import com.drrr.domain.techblogpost.entity.TechBlogPostCategory;
import com.drrr.domain.techblogpost.repository.TechBlogPostCategoryRepository;
import com.drrr.domain.techblogpost.repository.TechBlogPostRepository;
import com.drrr.domain.util.DatabaseCleaner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h3>Given</h3>
 * <h2>Member 데이터 생성</h2>
 * <br>Member Id M1, M2, M3, M4 생성</br>
 *
 * <h2>Post 생성</h2>
 * <br>Post Id P1~P100까지 생성</br>
 *
 * <h2>Category 생성</h2>
 * <br>Category Id C1~C10까지 생성</br>
 *
 * <h2>Category별 가중치 값 생성</h2>
 * <br>[C1-(8.0)], [C2-(15.0)], [C3-(5.0)], [C4-(5.0)], [C5-(1.0)]
 * <br>[M1~M4-C1,C2,C3,C4,C5]
 * <br> M1~M4가 선호하는 카테고리 [C1~C3]
 *
 * <h2>Member Id : 1 이 읽은 Post 목록 및 카테고리 생성</h2>
 * <br>Post Id 목록 : P1, P3, P5, P7, P9</br>
 * <br>[P1-C3,C5,C7], [P3-C2,C3,C7], [P5-C9], [P7-C4,C6,C9], [P9-C1,C2,C3]</br>
 * <br>그 외 나머지 Post는 P(2,4,6,8,10)-C2,C3,C5,C7,C8를 가지고 있고 P(11)~P(50) C-8로 통일</br>
 *
 * <h2>테스트 요소</h2>
 * - 시간에 따른 감소 - 가중치 최대값 초과 - 가중치 유효기간 체크(선호하는 카테고리, 선호하지 않는 카테고리 두가지) - 가중치의 최소값 미만 체크
 *
 * <h2>체크 요소</h2>
 * <br>M1 - 현재 가중치에서 DECREASE_WEIGHT의 세배 감소하는 것 체크</br>
 * <br>M2 - 현재 가중치가 MAX_WEIGHT를 한참 넘었을 때 MAX_WEIGHT로 바뀌는지 체크</br>
 * <br>M3 - 선호하는 가중치가 검증을 통해 최소값미만으로 안 내려가는 지 체크</br>
 * <br>M4 - 가중치가 0인 카테고리에 대해서는 category_weight 테이블에서 삭제되는지 체크</br>
 */

//
@DataJpaTest(includeFilters = @ComponentScan.Filter(Service.class))
@Import({QueryDSLConfiguration.class, DatabaseCleaner.class, JpaConfiguration.class})
class WeightValidationServiceTest {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TechBlogPostRepository techBlogPostRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryWeightRepository categoryWeightRepository;

    @Autowired
    private TechBlogPostCategoryRepository techBlogPostCategoryRepository;
    @Autowired
    private WeightValidationService weightValidationService;
    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Transactional
    @BeforeEach
    void setup() {
        databaseCleaner.clear();

        List<Member> members = IntStream.rangeClosed(0, 4).mapToObj(i -> {
            System.out.println("test test test");
            return Member.builder()
                    .email("example" + i + "@drrr.com")
                    .nickname("user" + i)
                    .provider("kakao" + i)
                    .providerId("12345" + i)
                    .build();
        }).toList();

        memberRepository.saveAll(members);

        List<TechBlogPost> techBlogPosts = IntStream.rangeClosed(1, 100).mapToObj(i -> {
            //현재로부터 몇년전까지 랜덤으로 연월일을 뽑을지 정함
            LocalDate createdDate = LocalDate.of(2023, 9, 30);

            String author = "Author" + i; // 짝수 인덱스에서만 저자 설정
            String thumbnailUrl = "http://example.com/thumbnail" + i + ".jpg";
            String title = "Title" + i;
            String summary = (i % 3 == 0) ? "Summary" + i : null; // 3의 배수 인덱스에서만 요약 설정
            String urlSuffix = "/suffix/" + i;
            String url = "http://example.com/suffix/" + i;
            TechBlogCode techBlogCode = TechBlogCode.values()[i
                    % TechBlogCode.values().length]; // 순환적으로 TechBlogCode 값 할당
            return TechBlogPost.builder()
                    .writtenAt(createdDate)
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

            String categoryDisplayName = "Display Category" + i;
            return Category.builder()
                    .name(categoryDisplayName)
                    .build();
        }).collect(Collectors.toList());
        categoryRepository.saveAll(categories);

        List<Category> categories1 = categoryRepository.findAll();

        List<Double> weights = Arrays.asList(8.0, 15.0, 5.0, 5.0, 1.0);
        List<CategoryWeight> categoryWeightList = new ArrayList<>();

        List<LocalDateTime> lastReadAtList = new ArrayList<>();
        lastReadAtList.add(LocalDateTime.now().minusHours(HoursConstants.PAST_HOURS.getValue() * 3L));
        lastReadAtList.add(LocalDateTime.now());
        lastReadAtList.add(LocalDateTime.now().minusHours(HoursConstants.PAST_HOURS.getValue() * 5L));
        lastReadAtList.add(LocalDateTime.now().minusDays(100));
        lastReadAtList.add(LocalDateTime.now().minusDays(100));

        IntStream.range(0, 5).forEach(i -> {
            IntStream.range(0, 5).forEach(j -> {
                LocalDateTime readAt = lastReadAtList.get(j);
                Category category = categories1.get(j);
                double value = weights.get(j);
                boolean preferred =
                        j == categories1.get(0).getId() || j == categories1.get(1).getId() || j == categories1.get(2)
                                .getId();

                categoryWeightList.add(CategoryWeight.builder()
                        .member(members.get(i))
                        .category(category)
                        .value(value)
                        .preferred(preferred)
                        .lastReadAt(readAt)
                        .build());
            });
        });

        categoryWeightRepository.saveAll(categoryWeightList);
        List<TechBlogPost> posts = techBlogPostRepository.findAll();
        List<Category> categoryList = categoryRepository.findAll();
        List<TechBlogPostCategory> techBlogPostCategories = new ArrayList<>();

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

        IntStream.rangeClosed(1, 7).forEach(j -> {
            Category category = categoryList.get(j);
            techBlogPostCategories.add(TechBlogPostCategory.builder()
                    .post(posts.get(1))
                    .category(category)
                    .build());
        });
        IntStream.rangeClosed(1, 7).forEach(j -> {
            Category category = categoryList.get(j);
            techBlogPostCategories.add(TechBlogPostCategory.builder()
                    .post(posts.get(3))
                    .category(category)
                    .build());
        });
        IntStream.rangeClosed(1, 7).forEach(j -> {
            Category category = categoryList.get(j);
            techBlogPostCategories.add(TechBlogPostCategory.builder()
                    .post(posts.get(5))
                    .category(category)
                    .build());
        });
        IntStream.rangeClosed(1, 7).forEach(j -> {
            Category category = categoryList.get(j);
            techBlogPostCategories.add(TechBlogPostCategory.builder()
                    .post(posts.get(7))
                    .category(category)
                    .build());
        });
        IntStream.rangeClosed(1, 7).forEach(j -> {
            Category category = categoryList.get(j);
            techBlogPostCategories.add(TechBlogPostCategory.builder()
                    .post(posts.get(9))
                    .category(category)
                    .build());
        });

        IntStream.rangeClosed(10, 49).forEach(i -> {
            TechBlogPost post = posts.get(i);
            Category category = categoryList.get(7);
            techBlogPostCategories.add(TechBlogPostCategory.builder()
                    .post(post)
                    .category(category)
                    .build());

        });

        techBlogPostCategoryRepository.saveAll(techBlogPostCategories);

    }


    @Test
    void 가중치_검증에_의해서_가중치가_제대로_감소합니다() {
        //when
        List<Member> members = memberRepository.findAll();
        if (members.isEmpty()) {
            throw new IllegalArgumentException("members elements is null");
        }

        weightValidationService.validateWeight(members.get(0).getId());
        weightValidationService.validateWeight(members.get(1).getId());
        weightValidationService.validateWeight(members.get(2).getId());
        weightValidationService.validateWeight(members.get(3).getId());

        List<CategoryWeight> member1CategoryWeight = categoryWeightRepository.findByMemberId(members.get(0).getId());

        if (member1CategoryWeight.isEmpty()) {
            throw new IllegalArgumentException("member1CategoryWeight elements is null");
        }
        List<CategoryWeight> member2CategoryWeight = categoryWeightRepository.findByMemberId(members.get(1).getId());
        if (member2CategoryWeight.isEmpty()) {
            throw new IllegalArgumentException("member2CategoryWeight elements is null");
        }
        List<CategoryWeight> member3CategoryWeight = categoryWeightRepository.findByMemberId(members.get(2).getId());
        if (member3CategoryWeight.isEmpty()) {
            throw new IllegalArgumentException("member3CategoryWeight elements is null");
        }
        List<CategoryWeight> member4CategoryWeight = categoryWeightRepository.findByMemberId(members.get(3).getId());

        //then
        assertThat(member1CategoryWeight.get(0).getValue()).isEqualTo(
                8.0 - (WeightConstants.DECREASE_WEIGHT.getValue() * 3));
        assertThat(member2CategoryWeight.get(1).getValue()).isEqualTo(WeightConstants.MAX_WEIGHT.getValue());
        assertThat(member3CategoryWeight.get(2).getValue()).isEqualTo(
                WeightConstants.MIN_CONDITIONAL_WEIGHT.getValue());
        //가중치가 0인 카테고리에 대해서는 데이터가 삭제됨
        assertThat(member4CategoryWeight.size()).isEqualTo(3);


    }

}