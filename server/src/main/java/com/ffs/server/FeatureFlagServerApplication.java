package com.ffs.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@MapperScan("com.ffs.server.mapper")
public class FeatureFlagServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeatureFlagServerApplication.class, args);
    }
}
