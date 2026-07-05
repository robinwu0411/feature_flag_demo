package com.ffs.sample;

import com.ffs.sdk.FeatureFlagClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Duration;

@SpringBootApplication
public class SampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @Bean(destroyMethod = "close")
    public FeatureFlagClient featureFlagClient() {
        String serverUrl = System.getenv().getOrDefault("FF_SERVER_URL", "http://localhost:8080");
        String appId = System.getenv().getOrDefault("FF_APP_ID", "sample-app");
        FeatureFlagClient client = FeatureFlagClient.builder()
                .serverUrl(serverUrl).appId(appId).syncInterval(Duration.ofSeconds(10)).build();
        client.start();
        return client;
    }
}
