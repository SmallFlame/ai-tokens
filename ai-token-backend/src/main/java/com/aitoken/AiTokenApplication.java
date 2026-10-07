package com.aitoken;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.aitoken.mapper")
@EnableAsync
@EnableScheduling
public class AiTokenApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiTokenApplication.class, args);
    }
}
