package com.drrr.domain.techblogpost.entity;

import com.drrr.core.code.TechBlogCode;
import com.drrr.domain.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "DRRR_TECHBLOGPOST")
@PrimaryKeyJoinColumn(name = "TECHBLOGPOST_ID")
public class TechBlogPost extends BaseEntity {

    @Column(nullable = false)
    private LocalDate createdDate;

    // naver의 경우 author가 없는 경우가 있음
    @Column
    private String author;
    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false)
    private String title;

    // 설명이 없는 기술블로그가 있을 수 있음
    @Column(length = 1000)
    private String summary;

    @Column(nullable = false)
    private String urlSuffix;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    private TechBlogCode techBlogCode;

    @Column(nullable = false)
    private int viewCount = 0;



    @Builder
    public TechBlogPost(LocalDate createdDate, String author, String thumbnailUrl, String title, String summary,
                        String urlSuffix, String url,
                        TechBlogCode crawlerGroup, int viewCount) {
        this.createdDate = createdDate;
        this.author = author;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;
        this.summary = summary;
        this.urlSuffix = urlSuffix;
        this.url = url;
        this.techBlogCode = crawlerGroup;
        this.viewCount = viewCount;
    }

    public static TechBlogPost from(TemporalTechBlogPost temporalTechBlogEntity) {
        return TechBlogPost.builder()
                .title(temporalTechBlogEntity.getTitle())
                .author(temporalTechBlogEntity.getAuthor())
                .crawlerGroup(temporalTechBlogEntity.getTechBlogCode())
                .createdDate(temporalTechBlogEntity.getCreatedDate())
                .url(temporalTechBlogEntity.getUrl())
                .summary(temporalTechBlogEntity.getSummary())
                .urlSuffix(temporalTechBlogEntity.getUrlSuffix())
                .build();
    }
    public static TechBlogPost increaseViewCount(TechBlogPost post){
        return TechBlogPost.builder()
                .title(post.getTitle())
                .author(post.getAuthor())
                .crawlerGroup(post.getTechBlogCode())
                .createdDate(post.getCreatedDate())
                .url(post.getUrl())
                .summary(post.getSummary())
                .urlSuffix(post.getUrlSuffix())
                .viewCount(post.getViewCount()+1)
                .build();
    }
}

