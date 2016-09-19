/*
 * Copyright (C) 2014 Motorola, Inc.
 * All Rights Reserved.
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package example.com.myapplication;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * A 1x1 pixel FrameLayout that can be used to create hidden UI elements.
 */
public class OverlayPixel extends FrameLayout {

    private final WindowManager.LayoutParams mLayoutParams;

    // This constructor is required to keep the Android layout editor (part of
    // Android Studio and Eclipse) happy.
    public OverlayPixel(Context context) {
        this(context, /* flags */ 0);
    }

    public OverlayPixel(Context context, int flags) {
        super(context);

        mLayoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | flags,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.RIGHT | Gravity.TOP;
    }

    /**
     * Provides the LayoutParams that should be used when adding this View to the Window Manager.
     */
    @Override
    public WindowManager.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }
}
