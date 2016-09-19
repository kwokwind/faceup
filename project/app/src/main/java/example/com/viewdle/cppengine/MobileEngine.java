/*
 * Copyright (C) 2014 Motorola, Inc.
 * All Rights Reserved.
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package example.com.viewdle.cppengine;
/**
 * This file was taken from Viewdle's face detection Demo App:
 * https://drive.google.com/a/motorola.com/file/d/0B_tJBWBWEjI2NjZ0OEhaYVktYXc/edit
 *
 * TODO(cbook): Work with Viewdle on improved library integration.
 */

import android.util.Log;

public class MobileEngine {
    private static final String TAG = "attentivedisplay.MobileEngine";
    private static MobileEngine m_singleton = null;
    private final boolean mIsLibraryLoaded;

    /*
     * Singleton implementation
     */
    public static MobileEngine GetMe() {
        if (m_singleton == null) {
            m_singleton = new MobileEngine();
        }
        return m_singleton;
    }

    private MobileEngine() {

        Log.d(TAG, "VdlFaceDetection loading...");
        boolean isLibraryLoaded = false;
        try {
            System.loadLibrary("vdlfacedetection_ad");
            isLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to find Viewdle library", e);
        }
        mIsLibraryLoaded = isLibraryLoaded;
        Log.d(TAG, "VdlFaceDetection loaded");
    }

    /**
     * Indicates if the native Viewdle library was successfully loaded.
     */
    public boolean isLibraryLoaded() {
        return mIsLibraryLoaded;
    }

    /**
     * Initializes the native Viewdle library with configuration parameters.
     */
    public native void VdlCreate(String configFolder);

    /**
     * Destroyed the native Viewdle library.
     */
    public native void VdlDestroy();

    /**
     * Performs face detection on an input image.
     */
    public native FacePosition[] VdlProcess(byte[] image, int width, int height);
}
