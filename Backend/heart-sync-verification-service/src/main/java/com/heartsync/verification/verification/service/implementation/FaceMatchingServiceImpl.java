package com.heartsync.verification.verification.service.implementation;

// import org.bytedeco.opencv.opencv_core.Mat;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;
import org.springframework.stereotype.Service;

import com.heartsync.verification.config.FaceMatchingProperties;
import com.heartsync.verification.verification.enums.FaceMatchFailureReason;
import com.heartsync.verification.verification.model.FaceMatchResult;
import com.heartsync.verification.verification.service.FaceMatchingService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class FaceMatchingServiceImpl implements FaceMatchingService {

    private final FaceMatchingProperties properties;

    @Override
    public FaceMatchResult compare(
            byte[] profileImage,
            byte[] liveSelfie) {

        Mat profileMat = decodeImage(profileImage);

        Mat selfieMat = decodeImage(liveSelfie);

        try {

            FaceDetectionResult profileFace = detectSingleFace(
                    profileMat,
                    true);

            if (!profileFace.valid()) {

                return failure(
                        profileFace.failureReason());
            }

            FaceDetectionResult selfieFace = detectSingleFace(
                    selfieMat,
                    false);

            if (!selfieFace.valid()) {

                return failure(
                        selfieFace.failureReason());
            }

            FaceRecognizerSF recognizer = FaceRecognizerSF.create(
                    properties.recognitionModel(),
                    "");

            Mat profileFeature = extractFeatures(
                    recognizer,
                    profileMat,
                    profileFace.face());

            Mat selfieFeature = extractFeatures(
                    recognizer,
                    selfieMat,
                    selfieFace.face());

            try {

                double similarity = recognizer.match(
                        profileFeature,
                        selfieFeature,
                        FaceRecognizerSF.FR_COSINE);

                boolean matched = similarity >= properties
                        .cosineThreshold();

                if (matched) {

                    return FaceMatchResult.success(
                            similarity,
                            properties.cosineThreshold(),
                            properties.modelVersion());
                }

                return FaceMatchResult.failure(
                        similarity,
                        properties.cosineThreshold(),
                        FaceMatchFailureReason.FACE_NOT_MATCHED,
                        properties.modelVersion());

            } finally {

                profileFeature.release();
                selfieFeature.release();
            }

        } finally {

            profileMat.release();
            selfieMat.release();
        }
    }

    private FaceDetectionResult detectSingleFace(
            Mat image,
            boolean profileImage) {

        FaceDetectorYN detector = FaceDetectorYN.create(
                properties.detectorModel(),
                "",
                new Size(
                        image.cols(),
                        image.rows()),
                (float) properties
                        .detectionThreshold(),
                (float) properties
                        .nmsThreshold(),
                properties.topK());

        detector.setInputSize(
                new Size(
                        image.cols(),
                        image.rows()));

        Mat faces = new Mat();

        detector.detect(
                image,
                faces);

        if (faces.rows() == 0) {

            faces.release();

            return new FaceDetectionResult(
                    false,
                    null,
                    profileImage
                            ? FaceMatchFailureReason.NO_FACE_IN_PROFILE_IMAGE
                            : FaceMatchFailureReason.NO_FACE_IN_SELFIE);
        }

        if (faces.rows() > 1) {

            faces.release();

            return new FaceDetectionResult(
                    false,
                    null,
                    profileImage
                            ? FaceMatchFailureReason.MULTIPLE_FACES_IN_PROFILE_IMAGE
                            : FaceMatchFailureReason.MULTIPLE_FACES_IN_SELFIE);
        }

        Mat face = faces.row(0).clone();

        faces.release();

        return new FaceDetectionResult(
                true,
                face,
                FaceMatchFailureReason.NONE);
    }

    private Mat extractFeatures(
            FaceRecognizerSF recognizer,
            Mat image,
            Mat face) {

        Mat alignedFace = new Mat();

        Mat features = new Mat();

        try {

            recognizer.alignCrop(
                    image,
                    face,
                    alignedFace);

            recognizer.feature(
                    alignedFace,
                    features);

            return features.clone();

        } finally {

            face.release();
            alignedFace.release();
            features.release();
        }
    }

    private Mat decodeImage(
            byte[] imageBytes) {

        if (imageBytes == null
                || imageBytes.length == 0) {

            throw new IllegalArgumentException(
                    "Image cannot be empty");
        }

        MatOfByte buffer = new MatOfByte(imageBytes);

        Mat image = Imgcodecs.imdecode(
                buffer,
                Imgcodecs.IMREAD_COLOR);

        buffer.release();

        if (image.empty()) {

            image.release();

            throw new IllegalArgumentException(
                    "Invalid image");
        }

        return image;
    }

    private FaceMatchResult failure(
            FaceMatchFailureReason reason) {

        return FaceMatchResult.failure(
                0.0,
                properties.cosineThreshold(),
                reason,
                properties.modelVersion());
    }

    private record FaceDetectionResult(

            boolean valid,

            Mat face,

            FaceMatchFailureReason failureReason

    ) {
    }
}
