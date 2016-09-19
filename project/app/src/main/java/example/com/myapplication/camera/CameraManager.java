/*
 * Copyright (C) 2014 Motorola, Inc.
 * All Rights Reserved.
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package example.com.myapplication.camera;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * Manages the camera preview functionality, including starting and stopping the camera, capturing
 * frames and providing buffers.
 */
public class CameraManager implements SurfaceHolder.Callback, Camera.PreviewCallback,
        FaceDetectionListener {

    /**
     * A callback interface for camera frame notifications.
     */
    public interface CameraFrameListener {
        public void onCameraError();
        public void onFaceDetection(Face[] faces);
    }

    private static final String TAG = "CameraManager";
    private static final String CAMERA_HANDLER_THREAD_NAME = "Camera";

    // Specifies the minimum preview image height we want to receive.
    private static final int MIN_PREVIEW_HEIGHT = 240;

    // Specifies the minimum preview image width we want to receive.
    private static final int MIN_PREVIEW_WIDTH = 320;

    // Number of preview frame buffers to allocate
    private static final int NUM_CAMERA_BUFFERS = 1;

    // Amount to over-expose for face detect.
    private static final int CAMERA_EXPOSURE_COMPENSATION = 0;

    private static final int CAME_MESSAGE_START = 0;
    private static final int CAME_MESSAGE_STOP = 1;
    private static final int CAME_MESSAGE_DESTROY = 2;

    private final SurfaceView mCameraView;
    private final CameraFrameListener mFrameListener;
    private final CameraInfo mCameraInfo;
    private final boolean mImageFlip;
    private final int mImageRotation;
    private final int mCameraId;
    private final HandlerThread mCameraThread;

    // Handler for invoking callbacks on the UI thread
    private final Handler mUiHandler;

    // Handler for the Camera thread.
    private final Handler mCameraHandler;

    // Members only accessed from Camera thread (except in constructor)
    private Camera mCamera;
    private int mPreviewWidth = -1;
    private int mPreviewHeight = -1;
    private boolean mFDIsRunning = false;
    private byte[][] mCameraBuffers;

    public CameraManager(SurfaceView cameraView, CameraFrameListener listener, Context context) {
        mFrameListener = listener;
        mCameraInfo = new CameraInfo();
        mCameraId = getCameraId();
        mImageRotation = mCameraInfo.orientation;
        mImageFlip = (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);

        mCameraView = cameraView;
        mCameraView.setVisibility(View.INVISIBLE);
        mCameraView.getHolder().addCallback(this);

        mUiHandler = new Handler();
        mCameraThread = new HandlerThread(CAMERA_HANDLER_THREAD_NAME);
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case CAME_MESSAGE_START:
                        Log.d(TAG, "CAME_MESSAGE_START");
                        startCamera();
                        break;
                    case CAME_MESSAGE_STOP:
                        Log.d(TAG, "CAME_MESSAGE_STOP");
                        stopCamera();
                        mCameraView.setVisibility(View.INVISIBLE);
                        break;
                    case CAME_MESSAGE_DESTROY:
                        Log.d(TAG, "CAME_MESSAGE_DESTROY");
                        mCameraThread.quitSafely();
                        break;
                    default:
                        Log.e(TAG, "unknown cam message " + inputMessage.what);
                        break;
                }
            }
        };
        Log.d(TAG, "camera thread id " + mCameraThread.getThreadId() + " is created!");
    }

    /**
     * Start the camera and frame capture.  Must be called from the UI thread.
     */
    public void resume() {
        mCameraView.setVisibility(View.VISIBLE);
    }

    /**
     * Stop the camera and frame capture.  Must be called from the UI thread.
     */
    public void pause() {
        // Closing the camera involves two steps:
        // 1. Stop the camera.
        // 2. Close the backing SurfaceView.
        // These steps need to be done in that order to avoid issues like
        // http://idart.mot.com/browse/IKFX-967
        //
        // Moreover, there are threading restrictions:
        // 1. Most camera operations need to happen on the thread that opened
        // the camera (which in our case is the camera thread).
        // 2. The backing SurfaceView needs to be hidden on the UI thread (due
        // to how Android works).
        //
        // All the above considerations result in the contraption below.
        if(mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.setFaceDetectionListener(null);
        }
        Message completeMessage = mCameraHandler.obtainMessage(CAME_MESSAGE_STOP, null);
        completeMessage.sendToTarget();
        /*
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                stopCamera();
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCameraView.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });*/
    }

    /**
     * Called on the Camera thread when the next preview frame is available.
     */
    @Override
    public void onPreviewFrame(byte[] frame, Camera camera) {
          if (mFrameListener != null) {
            if (mPreviewWidth == -1 || mPreviewHeight == -1) {
                Camera.Size size = camera.getParameters().getPreviewSize();
                mPreviewWidth = size.width;
                mPreviewHeight = size.height;
                Log.d(TAG, "preview width = " + mPreviewWidth + ", preview height = "
                        + mPreviewHeight);
            }

            if(!mFDIsRunning) {
                try {
                    mCamera.setFaceDetectionListener(this);
                    mCamera.startFaceDetection();
                    mFDIsRunning = true;
                    Log.d(TAG, "face detection is started!");
                } catch (RuntimeException e) {
                    Log.e(TAG, "can not start face detection", e);
                    throw e;
                }
            }

            /*
            if(frame != null) {
                dumpFiles(frame, 0);
            }
            */

            addPreviewBuffer(frame);
            mCamera.setPreviewCallbackWithBuffer(null);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }

    /**
     * Called on the UI thread when the camera surface has been created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Message completeMessage = mCameraHandler.obtainMessage(CAME_MESSAGE_START, null);
        completeMessage.sendToTarget();
        /*
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                startCamera();
            }
        });*/
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onFaceDetection(Face[] faces, Camera camera) {
        if (faces.length > 0) {
            final Face[] newFaces = faces;

            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mFrameListener.onFaceDetection(newFaces);
                }
            });
        }
    }

    /**
     * Called from the Camera thread to start the camera.
     */
    private void startCamera() {
        /*
         * In addition to a RuntimeException if the Camera cannot be opened (if it is busy), we may
         * still get RuntimeExceptions while configuring the camera if the service crashes or if the
         * UI thread hides the camera during startup.
         */
        try {
            mCamera = Camera.open(mCameraId);

            int degrees = mImageRotation;
            if (mImageFlip) {
                degrees = (360 - degrees) % 360;
            }
            mCamera.setDisplayOrientation(degrees);
            try {
                mCamera.setPreviewDisplay(mCameraView.getHolder());
            } catch (IOException e) {
                Log.e(TAG, "Error settings camera preview", e);
                return;
            }

            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            int maxDiff = Integer.MAX_VALUE;
            Camera.Size target = null;
            for (Camera.Size size : sizes) {
                if (size.width >= MIN_PREVIEW_WIDTH && size.height >= MIN_PREVIEW_HEIGHT) {
                    int diff = (size.width - MIN_PREVIEW_WIDTH)
                            + (size.height - MIN_PREVIEW_HEIGHT);
                    if (diff < maxDiff) {
                        maxDiff = diff;
                        target = size;
                    }
                }
            }
            if (target != null) {
                params.setPreviewSize(target.width, target.height);
            }

            Log.d(TAG, "the max number of detected faces is "+params.getMaxNumDetectedFaces());

            int exposureCompensation = Math.max(params.getMinExposureCompensation(), Math.min(
                    params.getMaxExposureCompensation(), CAMERA_EXPOSURE_COMPENSATION));
            Log.d(TAG, "Setting exposure comp to " + exposureCompensation);
            params.setExposureCompensation(exposureCompensation);
            params.setPreviewFormat(ImageFormat.NV21);
            mCamera.setParameters(params);
            mCamera.setPreviewCallbackWithBuffer(this);
            if (mCameraBuffers == null) {
                mCameraBuffers = new byte[NUM_CAMERA_BUFFERS][];
                for (int i = 0; i < NUM_CAMERA_BUFFERS; i++) {
                    mCameraBuffers[i] = new byte[getFrameSize()];
                }
            }
            for (int i = 0; i < NUM_CAMERA_BUFFERS; i++) {
                mCamera.addCallbackBuffer(mCameraBuffers[i]);
            }

            mCamera.startPreview();
            Log.d(TAG, "camera started!");
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to start camera", e);
            stopCamera();
            notifyFailed();
        }
    }

    /**
     * Called from the Camera thread to stop the camera.
     */
    private void stopCamera() {
        if (mCamera != null) {
            try {
                mCamera.setFaceDetectionListener(null);
                mCamera.stopFaceDetection();
                mFDIsRunning = false;

                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                Log.d(TAG, "stopCamera done!");
            } catch (RuntimeException e) {
                Log.w(TAG, "Failed to stop camera", e);
            }
        }
    }

    /**
     * Called from the client to ensure camera thread isn't leaking
     */
    public void destroy() {
        //mCameraThread.quitSafely();
        Message completeMessage = mCameraHandler.obtainMessage(CAME_MESSAGE_DESTROY, null);
        completeMessage.sendToTarget();
    }

    /**
     * Called from the camera thread to notify the client that the camera failed to open.
     */
    private void notifyFailed() {
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                mFrameListener.onCameraError();
            }
        });
    }

    /**
     * Called from the constructor to retrieve the id of the front-facing camera and populate
     * the CameraInfo.
     */
    private int getCameraId() {
        int numCameras = Camera.getNumberOfCameras();
        for (int i=0; i < numCameras; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Called from the Camera thread to calculate to appropriate frame buffer size.
     */
    private int getFrameSize() {
        Camera.Parameters params = mCamera.getParameters();
        int imgFormat = params.getPreviewFormat();
        int bitsPerPixel = ImageFormat.getBitsPerPixel(imgFormat);
        Camera.Size cameraSize = params.getPreviewSize();

        return ((cameraSize.width * cameraSize.height) * bitsPerPixel) / 8;
    }

    /**
     * Called from the Camera thread to provide a preview buffer for new frames to the camera.
     */
    private void addPreviewBuffer(byte[] buffer) {
        try {
            mCamera.addCallbackBuffer(buffer);
        } catch (RuntimeException e) {
            Log.w(TAG, "Error adding preview buffer", e);
        }
    }

    private void dumpFiles(byte[] imageData, int index) {
        File extStore = Environment.getExternalStorageDirectory();
        File myFile = new File(extStore.getAbsolutePath() + "/dump_rotated");
        if(myFile.exists()) {
            final String dumpFile = "/storage/emulated/legacy/data_"+index+".yuv";
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(dumpFile));
            } catch(FileNotFoundException e) {
                Log.d(TAG, "file can not be created with error"+e);
                return;
            }

            try {
                bos.write(imageData);
            } catch(IOException e) {
                Log.d(TAG, "file can not be wrote with error"+e);
            }

            try {
                bos.flush();
            } catch(IOException e) {
                Log.d(TAG, "file can not be flushed with error"+e);
            }

            try {
                bos.close();
            } catch(IOException e) {
                Log.d(TAG, "file can not be closed with error"+e);
            }
            Log.d(TAG, "dump file "+dumpFile+" is done!");
        }
    }
}
