package com.heartsync.verification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "verification.liveness")
public record LivenessProperties(
        String modelV2,

        String modelV1se,

        double realThreshold,

        int minFrames,

        int maxFrames,

        double requiredPassRatio,

        String modelVersion) {

}
