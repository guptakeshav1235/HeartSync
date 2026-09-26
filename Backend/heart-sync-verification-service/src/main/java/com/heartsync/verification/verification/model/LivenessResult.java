package com.heartsync.verification.verification.model;

import com.heartsync.verification.verification.enums.LivenessFailureReason;

public record LivenessResult(

        boolean passed,

        double score,

        LivenessFailureReason failureReason,

        String modelVersion) {

    public static LivenessResult success(
            double score,
            String modelVersion) {

        return new LivenessResult(
                true,
                score,
                LivenessFailureReason.NONE,
                modelVersion);
    }

    public static LivenessResult failure(
            double score,
            LivenessFailureReason reason,
            String modelVersion) {

        return new LivenessResult(
                false,
                score,
                reason,
                modelVersion);
    }
}
