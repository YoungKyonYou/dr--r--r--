package com.drrr.domain.techblogpost.entity;

import com.drrr.core.code.TechBlogCode;
import com.drrr.domain.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "DRRR_TEMP_TECH_BLOG_POST")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@PrimaryKeyJoinColumn(name = "TEMP_TECH_BLOG_POST_ID")
public class TemporalTechBlogPost extends BaseEntity {

    @Column(nullable = false)
    private LocalDate createdDate;

    @Column
    private String author;
    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false)
    private String title;

    // 설명이 없는 기술블로그가 있음
    @Column(length = 1000)
    private String summary;

    @Column(nullable = false)
    private String urlSuffix;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private TechBlogCode techBlogCode;


    /**
     * 크롤링 시작 일자. 관리자 앱에서 해당 일자를 기준으로 범위를 제한 함
     */
    @Column(nullable = false)
    private LocalDate crawledDate;

    @Column(nullable = false)
    private boolean registrationCompleted;

    @OneToMany(mappedBy = "temporalTechBlogPost")
    private List<TemporalTechPostTag> temporalTechPostTags = new ArrayList<>();

    @Builder
    public TemporalTechBlogPost(LocalDate createdDate,
                                String author,
                                String thumbnailUrl,
                                String title,
                                String summary,
                                String urlSuffix,
                                String url,
                                TechBlogCode techBlogCode,
                                LocalDate crawledDate,
                                boolean registrationCompleted,
                                List<TemporalTechPostTag> temporalTechPostTags
    ) {
        this.createdDate = createdDate;
        this.author = author;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;
        this.summary = summary;
        this.urlSuffix = urlSuffix;
        this.url = url;
        this.techBlogCode = techBlogCode;

        this.temporalTechPostTags = temporalTechPostTags;

        this.crawledDate = crawledDate;
        this.registrationCompleted = registrationCompleted;
    }

    public void registerCategory(List<TemporalTechPostTag> tags) {
        this.temporalTechPostTags = tags;
    }
}


