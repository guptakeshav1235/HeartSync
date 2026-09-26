package com.heartsync.verification.config;

import jakarta.annotation.PostConstruct;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

public class OpenCvConfig {

    @PostConstruct 
    public void initializeOpenCv() {

        Loader.load(
                opencv_java.class
        );
    }
}
      