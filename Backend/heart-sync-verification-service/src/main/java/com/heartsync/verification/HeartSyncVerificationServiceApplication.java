package com.heartsync.verification;

import com.heartsync.verification.config.FaceMatchingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(
        FaceMatchingProperties.class
)
public class HeartSyncVerificationServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                HeartSyncVerificationServiceApplication.class,
                args
        );
    }
}