package com.heartsync.verification.verification.service;

import com.heartsync.verification.verification.model.FaceMatchResult;

public interface FaceMatchingService {

    FaceMatchResult compare(
            byte[] profileImage,
            byte[] liveSelfie);
}
