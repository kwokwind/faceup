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

import android.graphics.Point;
import android.graphics.Rect;

public class FacePosition {
    private final int faceId;
    private final Rect rect = new Rect();
    private final Point left_eye = new Point();
    private final Point right_eye = new Point();
    private final Point nose = new Point();
    private final Point left_mouth = new Point();
    private final Point right_mouth = new Point();
    float smile;
    float opened_eyes;
    float closed_eyes;
    float dark_glasses;

    public FacePosition(int faceId, int rect_left, int rect_top, int rect_right, int rect_bottom,
            int left_eye_x, int left_eye_y, int right_eye_x, int right_eye_y, int nose_x,
            int nose_y, int left_mouth_x, int left_mouth_y, int right_mouth_x, int right_mouth_y,
            float smile, float opened_eyes, float closed_eyes, float dark_glasses) {
        super();
        this.faceId = faceId;
        this.rect.set(rect_left, rect_top, rect_right, rect_bottom);
        this.left_eye.set(left_eye_x, left_eye_y);
        this.right_eye.set(right_eye_x, right_eye_y);
        this.nose.set(nose_x, nose_y);
        this.left_mouth.set(left_mouth_x, left_mouth_y);
        this.right_mouth.set(right_mouth_x, right_mouth_y);
        this.smile = smile;
        this.opened_eyes = opened_eyes;
        this.closed_eyes = closed_eyes;
        this.dark_glasses = dark_glasses;
    }

    public int getFaceId() {
        return faceId;
    }

    public Rect getRect() {
        return rect;
    }

    public Point getLeftEye() {
        return left_eye;
    }

    public Point getRightEye() {
        return right_eye;
    }

    public Point getNose() {
        return nose;
    }

    public Point getLeftMouth() {
        return left_mouth;
    }

    public Point getRightMouth() {
        return right_mouth;
    }

    public float getSmile() {
        return smile;
    }

    // [-val, 0..100]
    public int getSmileValue() {
        return (int)(smile*100);
    }

    public float getOpenedEyes() {
        return opened_eyes;
    }

    public float getClosedEyes() {
        return closed_eyes;
    }

    // [-val, 0..100]
    public int getOpenedEyesValue() {
        if ((opened_eyes < 0) || (closed_eyes < 0)) {
            return -1;
        }
        return (int)((opened_eyes - closed_eyes) * 50) + 50;
    }

    public float getDarkGlasses() {
        return dark_glasses;
    }

    // [-val, 0..100]
    public int getDarkGlassesValue() {
        return (int)(dark_glasses*100);
    }

    public void update(FacePosition in) {
        smile = in.getSmile();
        opened_eyes = in.getOpenedEyes();
        closed_eyes = in.getClosedEyes();
        dark_glasses = in.getDarkGlasses();

        left_eye.x = in.getLeftEye().x * rect.width() / in.getRect().width();
        left_eye.y = in.getLeftEye().y * rect.height() / in.getRect().height();
        right_eye.x = in.getRightEye().x * rect.width() / in.getRect().width();
        right_eye.y = in.getRightEye().y * rect.height() / in.getRect().height();
        nose.x = in.getNose().x * rect.width() / in.getRect().width();
        nose.y = in.getNose().y* rect.height() / in.getRect().height();
        left_mouth.x = in.getLeftMouth().x * rect.width() / in.getRect().width();
        left_mouth.y = in.getLeftMouth().y* rect.height() / in.getRect().height();
        right_mouth.x = in.getRightMouth().x * rect.width() / in.getRect().width();
        right_mouth.y = in.getRightMouth().y* rect.height() / in.getRect().height();
    }
}
