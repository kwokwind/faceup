/*
 * Copyright (C) 2014 Motorola, Inc.
 * All Rights Reserved.
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package example.com.myapplication;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.content.pm.ActivityInfo;
import android.view.SurfaceView;
import android.view.WindowManager;

import example.com.myapplication.camera.CameraManager;
import example.com.myapplication.camera.CameraManager.CameraFrameListener;

/**
 * Implements the Face Detection Rotation Display logic:
 *   - Turns on the camera if screen is on.
 *   - Performs face detection on camera frames.
 *   - Rotate the screen to a correct position if a face is detected.
 */
public class MyFDRotationDisplay
        implements CameraFrameListener {

    private enum State {
        IDLE,
        WAIT_FOR_OBJECT,
        WAIT_FOR_FRAME
    }

    private static final String TAG = "MyFDRotationDisplay";
    private static final boolean DbgVerbose = false;

    private State mState;
    private final Context mContext;
    private final CameraManager mCameraManager;
    private final ScreenRotationOverlay mScreenRotationOverlay;
    private final OverlayPixel mOverlayLayout;
    private final WindowManager mWindowManager;

    /**
     * Creates the MyFDRotationDisplay instance and starts operation.
     */
    public MyFDRotationDisplay(Context context) {
        mContext = context;

        mOverlayLayout = new OverlayPixel(context, 0);
        mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(mOverlayLayout, mOverlayLayout.getLayoutParams());

        SurfaceView surfaceView = new SurfaceView(context);
        mOverlayLayout.addView(surfaceView);

        mCameraManager = new CameraManager(surfaceView, this, mContext);
        mScreenRotationOverlay = new ScreenRotationOverlay(mContext);

        mState = State.IDLE;
    }

    /**
     * Cleans up the MyFDRotationDisplay instance.
     */
    public void destroy() {
        Log.d(TAG, "destroy()");
        mState = State.IDLE;
        mCameraManager.pause();
        mCameraManager.destroy();
        mWindowManager.removeView(mOverlayLayout);
        mScreenRotationOverlay.destroy();
    }

    @Override
    public void onCameraError() {
        Log.d(TAG, "onCameraError()");
    }

    private int getOrientationByFace(Camera.Face face, boolean hasFaceID) {
        if(hasFaceID) {
            if (face.rightEye.x > face.mouth.x && face.rightEye.y > face.mouth.y) {
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            } else if (face.rightEye.x > face.mouth.x && face.rightEye.y < face.mouth.y) {
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            } else if (face.rightEye.x < face.mouth.x && face.rightEye.y < face.mouth.y) {
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            } else if (face.rightEye.x < face.mouth.x && face.rightEye.y > face.mouth.y) {
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            } else {
                return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            }
        } else {
            // to do ... analyze by rect info
            return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces) {
        if (faces.length <= 0) {
            Log.d(TAG, "no face detected...");
            return;
        }

        if(DbgVerbose) Log.d(TAG, faces.length + " faces are found!");

        int faceIndex = -1;
        boolean hasFaceID = false;

        for (int i = 0; i < faces.length; i++) {
                if(faces[i].rect != null ) {
                    faceIndex = i;
                    if(DbgVerbose) Log.d(TAG, "rect left, top, right, bottom are " + faces[i].rect.left +
                            ", " + faces[i].rect.top + ", " + faces[i].rect.right +
                            ", " + faces[i].rect.bottom);
                    break;
                }

                if(faces[i].id < 0 || faces[i].leftEye == null ||
                        faces[i].rightEye == null || faces[i].mouth == null)
                    Log.d(TAG, "face id or eyes/mouth coordinates are not supported");
                else {
                    faceIndex = i;
                    hasFaceID = true;
                    if(DbgVerbose) Log.d(TAG, "Face ID[" + faces[i].id
                            + "]: L " + faces[i].leftEye
                            + ", R " + faces[i].rightEye
                            + ", M " + faces[i].mouth);
                }
        }

        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if(faceIndex >=0) {
            orientation = getOrientationByFace(faces[faceIndex], hasFaceID);
        }

        if(orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            if(DbgVerbose) Log.d(TAG, "start to rotate screen to value " + orientation + "...");
            mScreenRotationOverlay.rotateScreen(orientation);
        }
    }

    public void startDetection() {
        Log.d(TAG, "startDetection() in state "+mState);
        if (mState == State.IDLE ) {
            mState = State.WAIT_FOR_FRAME;
            mCameraManager.resume();
        }
    }

    public void stopDetection() {
        Log.d(TAG, "stopDetection()");
        switch (mState) {
            case WAIT_FOR_OBJECT:
                break;
            case WAIT_FOR_FRAME:
                mCameraManager.pause();
                break;
            default:
                break;
        }
        mState = State.IDLE;
    }
}
