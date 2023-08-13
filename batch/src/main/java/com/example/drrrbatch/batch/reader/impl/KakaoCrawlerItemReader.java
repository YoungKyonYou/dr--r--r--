package com.example.drrrbatch.batch.reader.impl;

import com.example.drrrbatch.batch.domain.ExternalBlogPost;
import com.example.drrrbatch.batch.domain.ExternalBlogPosts;
import com.example.drrrbatch.batch.reader.AbstractCrawlerPageItemReader;
import com.example.drrrbatch.batch.reader.CrawlerPageStrategy;
import com.example.drrrbatch.batch.vo.TechBlogCode;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * page url format https://tech.kakao.com/blog/page/2/#posts {url}/blog/page/{}/#posts
 */
@Slf4j
public class KakaoCrawlerItemReader extends AbstractCrawlerPageItemReader {
    private static final TechBlogCode TECH_BLOG_CODE = TechBlogCode.KAKAO;
    private static final String PAGE_URL = "https://tech.kakao.com";
    private static final int PREFIX_LENGTH = PAGE_URL.length();
    private static final String PAGE_FORMAT = PAGE_URL + "/blog/page/%d/#posts";
    private static final String POST_CLASS_NAME = "elementor-posts--skin-classic";


    public KakaoCrawlerItemReader(WebDriver webDriver) {
        super(CrawlerPageStrategy.PAGE, webDriver);
    }

    @Override
    protected ExternalBlogPosts executeCrawlerPage() {
        this.selectPage();
        return new ExternalBlogPosts(this.webDriver.findElement(By.className(POST_CLASS_NAME))
                .findElements(By.tagName("article"))
                .stream().map(webElement -> {
                    final WebElement postElement = webElement.findElement(By.className("elementor-post__text"));
                    final WebElement postTitleElement = postElement.findElement(By.className("elementor-post__title"))
                            .findElement(By.tagName("a"));
                    final WebElement postMetaData = postElement.findElement(By.className("elementor-post__meta-data"));
                    final WebElement postAuthor = postMetaData.findElement(By.className("elementor-post-author"));
                    final WebElement postDate = postMetaData.findElement(By.className("elementor-post-date"));
                    final WebElement postSummary = postElement.findElement(By.className("elementor-post__excerpt")).findElement(By.tagName("p"));

                    return ExternalBlogPost.builder()
                            .code(TECH_BLOG_CODE)
                            .title(postTitleElement.getText())
                            .link(PAGE_URL)
                            .suffix(postTitleElement.getAttribute("href").substring(PREFIX_LENGTH))
                            .link(PAGE_URL)
                            .author(postAuthor.getText())
                            .summary(postSummary.getText())
                            .postDate(CrawlingLocalDatePatterns.PATTERN1.parse(postDate.getText()))
                            .build();
                }).toList());
    }


    @Override
    protected int getLastPage() {
        this.webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("page-numbers")));
        return this.webDriver.findElement(By.className("elementor-pagination"))
                .findElements(By.tagName("a"))
                .stream()
                .map(WebElement::getText)
                .filter(this::isNumber)
                .mapToInt(Integer::parseInt)
                .max()
                .orElseThrow();
    }

    @Override
    protected String getPageUrlByParameter(int page) {
        final String url = String.format(PAGE_FORMAT, page);
        log.info("crawler naver url: {}", url);
        return url;
    }

    public boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
