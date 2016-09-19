/*
 * Copyright (C) 2014 Motorola, Inc.
 * All Rights Reserved.
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package example.com.myapplication;

import android.app.Service;
import android.content.Context;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.graphics.PixelFormat;
import android.view.View;
import android.util.Log;
import static android.widget.LinearLayout.LayoutParams;

public class ScreenRotationOverlay {
    private static final String TAG = "ScreenRotationOverlay";

    private final WindowManager mWindowManager;
    private final LinearLayout mOrientationChanger;
    private final WindowManager.LayoutParams mOrientationLayout;

    public ScreenRotationOverlay(Context context) {
        mWindowManager = (WindowManager) context.getSystemService(Service.WINDOW_SERVICE);

        mOrientationChanger = new LinearLayout(context);
        mOrientationChanger.setClickable(false);
        mOrientationChanger.setFocusable(false);
        mOrientationChanger.setFocusableInTouchMode(false);
        mOrientationChanger.setLongClickable(false);

        mOrientationLayout = new WindowManager.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.RGBA_8888);

        mWindowManager.addView(mOrientationChanger, mOrientationLayout);
        mOrientationChanger.setVisibility(View.GONE);
    }

    /**
     * Cleans up ScreenControl.
     */
    public void destroy() {
        Log.d(TAG, "destroy the ScreenRotationOverlay...");
        //SystemClock.sleep(8000);
        mWindowManager.removeView(mOrientationChanger);
    }

    public void rotateScreen(int orientation) {
        if (orientation != 0xF && mOrientationLayout.screenOrientation != orientation) {
            Log.d(TAG, "rotate screen to "+orientation);
            mOrientationLayout.screenOrientation = orientation;/*ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;*/
            mWindowManager.updateViewLayout(mOrientationChanger, mOrientationLayout);
            mOrientationChanger.setVisibility(View.VISIBLE);
        }
    }
}
