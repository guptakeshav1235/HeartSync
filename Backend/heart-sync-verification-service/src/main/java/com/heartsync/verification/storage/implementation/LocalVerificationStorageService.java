package com.heartsync.verification.storage.implementation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

import com.heartsync.verification.storage.VerificationStorageService;

public class LocalVerificationStorageService implements VerificationStorageService {

    private final Path root;

    public LocalVerificationStorageService(
            @Value ("${verification.storage.root:./verification-storage}") String root) {

        this.root = Paths.get(root)
                .toAbsolutePath()
                .normalize();

        try {

            Files.createDirectories(
                    this.root);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to initialize verification storage",
                    exception);
        }
    }

    @Override
    public String storeProfileImage(
            UUID verificationUuid,
            byte[] image) {

        return store(
                verificationUuid,
                "profile",
                image);
    }

    @Override
    public String storeSelfie(
            UUID verificationUuid,
            byte[] image) {

        return store(
                verificationUuid,
                "selfie",
                image);
    }

    @Override
    public byte[] read(
            String reference) {

        Path path = resolveSafe(reference);

        try {

            return Files.readAllBytes(
                    path);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to read verification media",
                    exception);
        }
    }

    @Override
    public void delete(
            String reference) {

        if (reference == null) {
            return;
        }

        Path path = resolveSafe(reference);

        try {

            Files.deleteIfExists(
                    path);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to delete verification media",
                    exception);
        }
    }

    private String store(
            UUID verificationUuid,
            String type,
            byte[] bytes) {

        if (bytes == null
                || bytes.length == 0) {

            throw new IllegalArgumentException(
                    "Verification image is empty");
        }

        Path directory = root.resolve(
                verificationUuid.toString());

        try {

            Files.createDirectories(
                    directory);

            String fileName = type
                    + "-"
                    + UUID.randomUUID();

            Path target = directory.resolve(
                    fileName);

            Files.write(
                    target,
                    bytes,
                    StandardOpenOption.CREATE_NEW);

            return root
                    .relativize(target)
                    .toString();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to store verification image",
                    exception);
        }
    }

    private Path resolveSafe(
            String reference) {

        Path resolved = root.resolve(reference)
                .normalize();

        if (!resolved.startsWith(root)) {

            throw new SecurityException(
                    "Invalid storage reference");
        }

        return resolved;
    }
}
