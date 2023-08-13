package com.example.drrrbatch.batch.config.crawler;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChromeDriverPathProperty.class)
public class ChromeDriverConfiguration {

    @Bean
    public WebDriver webDriver(ChromeDriverPathProperty chromeDriverPathProperty) {
        System.setProperty("webdriver.chrome.driver", chromeDriverPathProperty.getDriverPath());

        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");                       //브라우저 안띄움
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
        options.addArguments("--disable-gpu");            //gpu 비활성화
        options.addArguments("--blink-settings=imagesEnabled=false"); //이미지 다운 안받음
        options.addArguments("—user-data-dir=" + System.getProperty("java.io.tmpdir")); // 종료 옵션 추가
        return new ChromeDriver(options);
    }
}
