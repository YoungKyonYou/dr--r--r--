package com.example.drrrbatch.batch.config.crawler;

import com.example.drrrbatch.batch.config.CrawlingBatchConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableConfigurationProperties(ChromeDriverPathProperty.class)
@ConditionalOnBean(CrawlingBatchConfiguration.class)
public class ChromeDriverConfiguration {

    @Bean
    public WebDriver webDriver(ChromeDriverPathProperty chromeDriverPathProperty) {
//        System.setProperty("webdriver.chrome.driver", chromeDriverPathProperty.getDriverPath());
//
//        ChromeOptions options = new ChromeOptions();
//        options.addArguments("headless");                       //브라우저 안띄움
//        options.addArguments("--remote-allow-origins=*");
//        options.addArguments("--disable-popup-blocking");       //팝업안띄움
//        options.addArguments("--disable-gpu");            //gpu 비활성화
//        options.addArguments("--blink-settings=imagesEnabled=false"); //이미지 다운 안받음
//        options.addArguments("—user-data-dir=" + System.getProperty("java.io.tmpdir")); // 종료 옵션 추가
//
//        return new ChromeDriver(options);

        FirefoxOptions options = new FirefoxOptions();
        return new FirefoxDriver(options);
    }


}
