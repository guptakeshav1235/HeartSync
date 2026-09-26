package com.heartsync.verification.verification.service.implementation;

import java.nio.FloatBuffer;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.springframework.stereotype.Service;

import com.heartsync.verification.config.FaceMatchingProperties;
import com.heartsync.verification.config.LivenessProperties;
import com.heartsync.verification.verification.enums.LivenessFailureReason;
import com.heartsync.verification.verification.model.LivenessResult;
import com.heartsync.verification.verification.service.LivenessService;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class LivenessServiceImpl implements LivenessService {

    private static final int INPUT_WIDTH = 80;
    private static final int INPUT_HEIGHT = 80;

    /*
     * MiniFASNet classes:
     *
     * 0 = paper photo
     * 1 = real face
     * 2 = screen photo
     */
    private static final int REAL_CLASS = 1;

    private final LivenessProperties properties;

    private final FaceMatchingProperties faceProperties;

    private OrtEnvironment environment;

    private OrtSession modelV2;

    private OrtSession modelV1se;

    @PostConstruct 
    public void initialize() {

        try {

            environment =
                    OrtEnvironment.getEnvironment();

            OrtSession.SessionOptions options =
                    new OrtSession.SessionOptions();

            modelV2 =
                    environment.createSession(
                            properties.modelV2(),
                            options
                    );

            modelV1se =
                    environment.createSession(
                            properties.modelV1se(),
                            options
                    );

        } catch (OrtException exception) {

            throw new IllegalStateException(
                    "Unable to initialize liveness models",
                    exception
            );
        }
    }

    @Override
    public LivenessResult verify(
            List<byte[]> frames
    ) {

        if (frames == null
                || frames.size()
                < properties.minFrames()) {

            return failure(
                    LivenessFailureReason
                            .TOO_FEW_FRAMES
            );
        }

        if (frames.size()
                > properties.maxFrames()) {

            return failure(
                    LivenessFailureReason
                            .TOO_MANY_FRAMES
            );
        }

        double totalRealScore = 0.0;

        int passedFrames = 0;

        for (byte[] frame : frames) {

            FrameResult result =
                    analyzeFrame(frame);

            if (!result.valid()) {

                return LivenessResult.failure(
                        0.0,
                        result.failureReason(),
                        properties.modelVersion()
                );
            }

            totalRealScore +=
                    result.realScore();

            if (result.realScore()
                    >= properties.realThreshold()) {

                passedFrames++;
            }
        }

        double averageScore =
                totalRealScore
                        / frames.size();

        double passRatio =
                (double) passedFrames
                        / frames.size();

        boolean passed =
                averageScore
                        >= properties.realThreshold()
                &&
                passRatio
                        >= properties.requiredPassRatio();

        if (!passed) {

            return LivenessResult.failure(
                    averageScore,
                    LivenessFailureReason
                            .SPOOF_DETECTED,
                    properties.modelVersion()
            );
        }

        return LivenessResult.success(
                averageScore,
                properties.modelVersion()
        );
    }

    private FrameResult analyzeFrame(
            byte[] imageBytes
    ) {

        Mat image =
                decodeImage(imageBytes);

        if (image == null) {

            return FrameResult.failure(
                    LivenessFailureReason
                            .INVALID_IMAGE
            );
        }

        try {

            FaceBoxResult faceResult =
                    detectSingleFace(image);

            if (!faceResult.valid()) {

                return FrameResult.failure(
                        faceResult.failureReason()
                );
            }

            double[] v2 =
                    predict(
                            image,
                            faceResult.box(),
                            modelV2,
                            2.7
                    );

            double[] v1se =
                    predict(
                            image,
                            faceResult.box(),
                            modelV1se,
                            4.0
                    );

            double realScore =
                    (
                            v2[REAL_CLASS]
                            +
                            v1se[REAL_CLASS]
                    ) / 2.0;

            return FrameResult.success(
                    realScore
            );

        } finally {

            image.release();
        }
    }

    private FaceBoxResult detectSingleFace(
            Mat image
    ) {

        FaceDetectorYN detector =
                FaceDetectorYN.create(
                        faceProperties.detectorModel(),
                        "",
                        new Size(
                                image.cols(),
                                image.rows()
                        ),
                        (float)
                                faceProperties
                                        .detectionThreshold(),
                        (float)
                                faceProperties
                                        .nmsThreshold(),
                        faceProperties.topK()
                );

        Mat faces =
                new Mat();

        try {

            detector.detect(
                    image,
                    faces
            );

            if (faces.rows() == 0) {

                return FaceBoxResult.failure(
                        LivenessFailureReason.NO_FACE
                );
            }

            if (faces.rows() > 1) {

                return FaceBoxResult.failure(
                        LivenessFailureReason
                                .MULTIPLE_FACES
                );
            }

            double[] values =
                    faces.get(0, 0);

            Rect box =
                    new Rect(
                            (int) values[0],
                            (int) values[1],
                            (int) values[2],
                            (int) values[3]
                    );

            return FaceBoxResult.success(
                    box
            );

        } finally {

            faces.release();
        }
    }

    private double[] predict(
            Mat image,
            Rect faceBox,
            OrtSession session,
            double scale
    ) {

        Mat processed =
                prepareInput(
                        image,
                        faceBox,
                        scale
                );

        try {

            float[] chw =
                    toChw(processed);

            long[] shape = {
                    1,
                    3,
                    INPUT_HEIGHT,
                    INPUT_WIDTH
            };

            try (
                    OnnxTensor tensor =
                            OnnxTensor.createTensor(
                                    environment,
                                    FloatBuffer.wrap(chw),
                                    shape
                            )
            ) {

                String inputName =
                        session.getInputNames()
                                .iterator()
                                .next();

                try (
                        OrtSession.Result result =
                                session.run(
                                        java.util.Map.of(
                                                inputName,
                                                tensor
                                        )
                                )
                ) {

                    Object value =
                            result.get(0)
                                    .getValue();

                    float[] logits;

                    if (value instanceof float[][] output) {

                        logits =
                                output[0];

                    } else {

                        throw new IllegalStateException(
                                "Unexpected liveness model output"
                        );
                    }

                    return softmax(logits);
                }

            } catch (OrtException exception) {

                throw new IllegalStateException(
                        "Liveness inference failed",
                        exception
                );
            }

        } finally {

            processed.release();
        }
    }

    private Mat prepareInput(
            Mat image,
            Rect faceBox,
            double scale
    ) {

        Rect expanded =
                expandBox(
                        image.cols(),
                        image.rows(),
                        faceBox,
                        scale
                );

        Mat cropped =
                new Mat(
                        image,
                        expanded
                );

        Mat resized =
                new Mat();

        try {

            Imgproc.resize(
                    cropped,
                    resized,
                    new Size(
                            INPUT_WIDTH,
                            INPUT_HEIGHT
                    )
            );

            return resized;

        } finally {

            cropped.release();
        }
    }

    private Rect expandBox(
            int imageWidth,
            int imageHeight,
            Rect box,
            double scale
    ) {

        double centerX =
                box.x + box.width / 2.0;

        double centerY =
                box.y + box.height / 2.0;

        int width =
                (int) Math.min(
                        box.width * scale,
                        imageWidth
                );

        int height =
                (int) Math.min(
                        box.height * scale,
                        imageHeight
                );

        int x =
                (int) (
                        centerX
                        -
                        width / 2.0
                );

        int y =
                (int) (
                        centerY
                        -
                        height / 2.0
                );

        x = Math.max(
                0,
                Math.min(
                        x,
                        imageWidth - width
                )
        );

        y = Math.max(
                0,
                Math.min(
                        y,
                        imageHeight - height
                )
        );

        return new Rect(
                x,
                y,
                width,
                height
        );
    }

    private float[] toChw(
            Mat image
    ) {

        int pixels =
                INPUT_WIDTH
                        * INPUT_HEIGHT;

        byte[] bgr =
                new byte[pixels * 3];

        image.get(
                0,
                0,
                bgr
        );

        float[] chw =
                new float[pixels * 3];

        for (int i = 0;
             i < pixels;
             i++) {

            /*
             * MiniFASNet expects BGR.
             *
             * Do NOT convert to RGB here.
             */

            chw[i] =
                    bgr[i * 3]
                            & 0xFF;

            chw[pixels + i] =
                    bgr[i * 3 + 1]
                            & 0xFF;

            chw[pixels * 2 + i] =
                    bgr[i * 3 + 2]
                            & 0xFF;
        }

        return chw;
    }

    private double[] softmax(
            float[] logits
    ) {

        double max =
                Double.NEGATIVE_INFINITY;

        for (float value : logits) {
            max = Math.max(
                    max,
                    value
            );
        }

        double sum = 0.0;

        double[] result =
                new double[logits.length];

        for (int i = 0;
             i < logits.length;
             i++) {

            result[i] =
                    Math.exp(
                            logits[i] - max
                    );

            sum += result[i];
        }

        for (int i = 0;
             i < result.length;
             i++) {

            result[i] =
                    result[i] / sum;
        }

        return result;
    }

    private Mat decodeImage(
            byte[] bytes
    ) {

        if (bytes == null
                || bytes.length == 0) {

            return null;
        }

        MatOfByte buffer =
                new MatOfByte(bytes);

        try {

            Mat image =
                    Imgcodecs.imdecode(
                            buffer,
                            Imgcodecs.IMREAD_COLOR
                    );

            if (image.empty()) {

                image.release();

                return null;
            }

            return image;

        } finally {

            buffer.release();
        }
    }

    private LivenessResult failure(
            LivenessFailureReason reason
    ) {

        return LivenessResult.failure(
                0.0,
                reason,
                properties.modelVersion()
        );
    }

    @PreDestroy 
    public void close() {

        try {

            if (modelV2 != null) {
                modelV2.close();
            }

            if (modelV1se != null) {
                modelV1se.close();
            }

        } catch (OrtException exception) {

            throw new IllegalStateException(
                    "Unable to close ONNX sessions",
                    exception
            );
        }
    }

    private record FaceBoxResult(
            boolean valid,
            Rect box,
            LivenessFailureReason failureReason
    ) {

        static FaceBoxResult success(
                Rect box
        ) {

            return new FaceBoxResult(
                    true,
                    box,
                    LivenessFailureReason.NONE
            );
        }

        static FaceBoxResult failure(
                LivenessFailureReason reason
        ) {

            return new FaceBoxResult(
                    false,
                    null,
                    reason
            );
        }
    }

    private record FrameResult(
            boolean valid,
            double realScore,
            LivenessFailureReason failureReason
    ) {

        static FrameResult success(
                double score
        ) {

            return new FrameResult(
                    true,
                    score,
                    LivenessFailureReason.NONE
            );
        }

        static FrameResult failure(
                LivenessFailureReason reason
        ) {

            return new FrameResult(
                    false,
                    0.0,
                    reason
            );
        }
    }
}
