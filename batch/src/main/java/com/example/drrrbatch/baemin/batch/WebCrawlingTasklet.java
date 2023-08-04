package com.example.drrrbatch.baemin.batch;



import com.example.drrrbatch.baemin.entity.BaeMinEntity;
import com.example.drrrbatch.baemin.repository.BaeMinRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.Duration;
import java.util.ArrayList;

import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.openqa.selenium.support.ui.ExpectedConditions.textToBe;

@RequiredArgsConstructor
@Slf4j
public class WebCrawlingTasklet implements Tasklet, StepExecutionListener {
    private final BaeMinRepository baeMinRepository;
    private EntityManager entityManager;
    private int page;
    private String lastPage = "";
    private WebDriver driver;
    private WebDriverWait wait;

    public WebCrawlingTasklet(BaeMinRepository baeMinRepository, EntityManagerFactory entityManagerFactory) {
        this.baeMinRepository = baeMinRepository;
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        page = 1;
        Long count = baeMinRepository.count();

        if (count == 0) {
            chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("condition", "CONTINUE");
            return RepeatStatus.FINISHED;
        }


        /////////////////////////////////////////////
        ////////////////////////////////////////////
        createDriver();

        List<String> postUrls = new ArrayList<>();

        while (page <= Integer.parseInt(lastPage) + 1) {

            page++;


            //해당 페이지의 블로그 글 읽어오기기
            WebElement postsElement = driver.findElement(By.className("posts"));
            List<WebElement> items = postsElement.findElements(By.className("item"));

            // 각 'item' 요소에서 a 태그의 내용을 가져옵니다.
            for (WebElement item : items) {
                String className = item.getAttribute("class");
                if (className.equals("item")) {
                    WebElement aTag = item.findElement(By.tagName("a"));
                    List<WebElement> spanTag = aTag.findElements(By.tagName("span"));
                    List<WebElement> pTag = aTag.findElements(By.tagName("p"));

                    String date = spanTag.get(0).getText();
                    String author = spanTag.get(1).getText();
                    String thumbnailUrl = "없음";
                    if (spanTag.size() == 3) {
                        thumbnailUrl = spanTag.get(2).findElement(By.tagName("img")).getAttribute("src");
                    }

                    String title = aTag.findElement(By.tagName("h1")).getText();
                    String summary = pTag.get(1).getText();
                    String hrefContent = aTag.getAttribute("href");
                    String postId = Pattern.compile("\\d+")
                            .matcher(hrefContent)
                            .results()
                            .map(MatchResult::group)
                            .findFirst().get();

                    postUrls.add(hrefContent);

                    BaeMinEntity byPostId = baeMinRepository.findByPostId(postId);

                    //DB에 넣을 최신 블로그 글이 없는 경우
                    if (byPostId != null) {
                        closeDriver();
                        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("condition", "COMPLETED");
                        return RepeatStatus.FINISHED;
                    }

                    BaeMinEntity baeMinTechBlog = BaeMinEntity.builder()
                            .date(date)
                            .author(author)
                            .thumbnailUrl(thumbnailUrl)
                            .title(title)
                            .summary(summary)
                            .postId(postId)
                            .url(hrefContent)
                            .catalogues("")
                            .build();
                    baeMinRepository.save(baeMinTechBlog);
                }
            }

            //마지막 페이지를 끝냈을 대 page는 이미 lastPage +1인 상태여서 여기서 break
            if (page == Integer.parseInt(lastPage) + 1) {
                break;
            }

            WebElement targetPageElement = driver.findElement(
                    By.cssSelector("a[title='" + String.valueOf(page) + " 쪽']"));

            // 요소를 클릭합니다.
            if (targetPageElement != null) {
                targetPageElement.click();
                System.out.println("현재 페이지:" + page);
                wait.until(ExpectedConditions.and(
                        ExpectedConditions.visibilityOfElementLocated(By.className("current")),
                        ExpectedConditions.textToBePresentInElementLocated(By.className("current"),
                                String.valueOf(page))
                ));
            } else {
                System.out.println("해당 페이지 요소를 찾을 수 없습니다.");
                break;
            }
        }

        //Socket Error 방지용
        closeDriver();

        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("condition", "COMPLETED");
        return RepeatStatus.FINISHED;

    }

    private void createDriver(){
        System.setProperty("webdriver.chrome.driver", "C:/Users/USER/chrome/chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
        options.addArguments("headless");                       //브라우저 안띄움
        options.addArguments("--disable-gpu");            //gpu 비활성화
        options.addArguments("--blink-settings=imagesEnabled=false"); //이미지 다운 안받음
        driver = new ChromeDriver(options);

        driver.get("https://techblog.woowahan.com/");
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));


        // 끝 페이지로 이동
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("last")));
        WebElement target = driver.findElement(By.className("last"));
        target.click();

        //끝 페이지에서
        wait.until(ExpectedConditions.not(textToBe(By.className("current"), String.valueOf(1))));
        lastPage = driver.findElement(By.className("current")).getText().toString();
        System.out.println("lastPage = " + lastPage);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("first")));
        target = driver.findElement(By.className("first"));
        target.click();
        wait.until(ExpectedConditions.and(
                ExpectedConditions.visibilityOfElementLocated(By.className("current")),
                ExpectedConditions.textToBePresentInElementLocated(By.className("current"), String.valueOf(page))
        ));
    }

    private void closeDriver(){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        driver.quit();
    }


    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("===========Tasklet Start=========");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Line Reader ended.");
        return ExitStatus.COMPLETED;
    }


}
