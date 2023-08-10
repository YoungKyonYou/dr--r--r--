package com.example.drrrbatch.batch.reader.impl;

import com.example.drrrbatch.batch.domain.ExternalBlogPost;
import com.example.drrrbatch.batch.domain.ExternalBlogPosts;
import com.example.drrrbatch.batch.reader.AbstractCrawlerPageItemReader;
import com.example.drrrbatch.batch.reader.CrawlerPageStrategy;
import com.example.drrrbatch.batch.vo.TechBlogCode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

@Slf4j
public class MarketKurlyItemReader extends AbstractCrawlerPageItemReader {
    private static final String BLOG_PREFIX = "https://helloworld.kurly.com/blog";
    private static final TechBlogCode code = TechBlogCode.MARKET_KURLY;

    public MarketKurlyItemReader(WebDriver webDriver) {
        super(CrawlerPageStrategy.SINGLE_PAGE, webDriver);
    }

    @Override
    protected ExternalBlogPosts executeCrawlerPage() {
        log.info("run market kurly crawler");
        this.webDriver.get("https://helloworld.kurly.com/");
        // 페이지 로딩을 위해 약간의 시간 대기 (이 부분은 선택 사항)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }

        //https://helloworld.kurly.com/blog
        var crawlerResult = webDriver.findElement(By.className("post-list"))
                .findElements(By.className("post-card"))
                .stream().map(webElement -> {
                    var postLink = EmptyFinder.get(() -> webElement.findElement(By.tagName("a")))
                            .orElseThrow(IllegalArgumentException::new);
                    var postTitle = EmptyFinder.get(() -> postLink.findElement(By.className("post-title")).getText()).get();
                    var postSummary = EmptyFinder.get(() -> postLink.findElement(By.className("title-summary")).getText()).orElse("");
                    var postMeta = EmptyFinder.get(() -> webElement.findElement(By.className("post-meta"))).get();
                    var postAuthor = EmptyFinder.get(() -> postMeta.findElement(By.className("post-autor")).getText()).orElse("");
                    var postDate = EmptyFinder.get(() -> postMeta.findElement(By.className("post-date")).getText()).orElse("");

                    return ExternalBlogPost.builder()
                            .title(postTitle)
                            .summary(postSummary)
                            .author(postAuthor)
                            .postDate(LocalDate.parse(postDate, FORMATTER2))
                            .link(BLOG_PREFIX)
                            .suffix(postLink.getAttribute("href").substring(BLOG_PREFIX.length() + 1))
                            .code(code)
                            .build();
                }).toList();

        return new ExternalBlogPosts(crawlerResult);
    }

    static class EmptyFinder {
        static <R> Optional<R> get(Supplier<R> result) {
            try {
                return Optional.of(result.get());
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }
}