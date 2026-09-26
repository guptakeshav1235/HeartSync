package com.heartsync.verification.verification.model;

import com.heartsync.verification.verification.enums.FaceMatchFailureReason;

public record FaceMatchResult(

        boolean matched,

        double score,

        double threshold,

        FaceMatchFailureReason failureReason,

        String modelVersion) {

    public static FaceMatchResult success(
            double score,
            double threshold,
            String modelVersion) {

        return new FaceMatchResult(
                true,
                score,
                threshold,
                FaceMatchFailureReason.NONE,
                modelVersion);
    }

    public static FaceMatchResult failure(
            double score,
            double threshold,
            FaceMatchFailureReason reason,
            String modelVersion) {

        return new FaceMatchResult(
                false,
                score,
                threshold,
                reason,
                modelVersion);
    }
}
