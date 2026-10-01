/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.app.Application;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.Display;

import vendor.xiaomi.hw.touchfeature.ITouchFeature;

/**
 * Like HyperOS, tell the touch panel which way the screen is rotated so it
 * applies the matching edge palm rejection.
 */
public class PianoPartsApp extends Application {

    private static final String TAG = "PianoParts";

    private static final String TOUCH_FEATURE_SERVICE =
            "vendor.xiaomi.hw.touchfeature.ITouchFeature/default";
    private static final int TOUCH_ID = 0;
    private static final int MODE_PANEL_ORIENTATION = 8;

    private DisplayManager mDisplayManager;
    private ITouchFeature mTouchFeature;
    private int mRotation = -1;

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {}

                @Override
                public void onDisplayRemoved(int displayId) {}

                @Override
                public void onDisplayChanged(int displayId) {
                    if (displayId == Display.DEFAULT_DISPLAY) {
                        updateRotation();
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        mDisplayManager = getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(mDisplayListener,
                new Handler(Looper.getMainLooper()));
        updateRotation();
    }

    private void updateRotation() {
        Display display = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            return;
        }
        int rotation = display.getRotation();
        if (rotation == mRotation) {
            return;
        }
        ITouchFeature touchFeature = getTouchFeature();
        if (touchFeature == null) {
            return;
        }
        try {
            // Surface.ROTATION_* values match the panel's orientation values.
            touchFeature.setTouchMode(TOUCH_ID, MODE_PANEL_ORIENTATION, rotation);
            mRotation = rotation;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set the touch panel orientation", e);
            mTouchFeature = null;
        }
    }

    private ITouchFeature getTouchFeature() {
        if (mTouchFeature == null) {
            IBinder binder = ServiceManager.waitForDeclaredService(TOUCH_FEATURE_SERVICE);
            if (binder == null) {
                Log.e(TAG, "TouchFeature service is not available");
                return null;
            }
            mTouchFeature = ITouchFeature.Stub.asInterface(binder);
        }
        return mTouchFeature;
    }
}
