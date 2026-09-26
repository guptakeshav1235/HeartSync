package com.heartsync.verification.storage;

import java.util.UUID;

public interface VerificationStorageService {

    String storeProfileImage(
            UUID verificationUuid,
            byte[] image);

    String storeSelfie(
            UUID verificationUuid,
            byte[] image);

    byte[] read(
            String reference);

    void delete(
            String reference);
}
