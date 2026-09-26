package com.heartsync.verification.verification.service;

import java.util.List;

import com.heartsync.verification.verification.model.LivenessResult;

public interface LivenessService {

    LivenessResult verify(
            List<byte[]> frames);
}
