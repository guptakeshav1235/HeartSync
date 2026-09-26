package com.heartsync.verification.config;

public record FaceMatchingProperties(

        String detectorModel,

        String recognitionModel,

        double detectionThreshold,

        double nmsThreshold,

        int topK,

        double cosineThreshold,

        String modelVersion
) {
}
