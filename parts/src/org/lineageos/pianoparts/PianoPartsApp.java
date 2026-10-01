/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
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
    private static final int MODE_GAME = 0;
    private static final int MODE_PANEL_ORIENTATION = 8;

    private static final String DISPLAY_FEATURE_SERVICE =
            "vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature/default";
    private static final String DISPLAY_FEATURE_DESCRIPTOR =
            "vendor.xiaomi.hardware.displayfeature_aidl.IDisplayFeature";
    // IDisplayFeature.setFeature(displayId, featureId, value, cookie)
    private static final int TRANSACTION_SET_FEATURE = IBinder.FIRST_CALL_TRANSACTION + 6;
    private static final int FEATURE_PAPER_MODE = 31;
    private static final int FEATURE_TRUE_TONE = 32;
    static final int FEATURE_SUNLIGHT_SCREEN = 12;

    private static final String PREFS_NAME = "piano_parts";
    private static final String KEY_TOUCH_GAME_MODE = "touch_game_mode";
    private static final String KEY_READING_MODE = "reading_mode";
    private static final String KEY_TRUE_TONE = "true_tone";
    private static final String KEY_SUNLIGHT_MODE = "sunlight_mode";

    private DisplayManager mDisplayManager;
    private SunlightModeController mSunlightModeController;
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
        if (isTouchGameMode()) {
            applyTouchGameMode(true);
        }
        if (isReadingMode()) {
            setDisplayFeature(FEATURE_PAPER_MODE, 1);
        }
        if (isTrueTone()) {
            setDisplayFeature(FEATURE_TRUE_TONE, 1);
        }
        new PenBatteryNotifier(this);
        DefaultWallpaper.applyOnce(this, getPrefs());
        mSunlightModeController = new SunlightModeController(this);
        mSunlightModeController.setEnabled(isSunlightMode());
    }

    private SharedPreferences getPrefs() {
        Context context = createDeviceProtectedStorageContext();
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    boolean isTouchGameMode() {
        return getPrefs().getBoolean(KEY_TOUCH_GAME_MODE, false);
    }

    void setTouchGameMode(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_TOUCH_GAME_MODE, enabled).apply();
        applyTouchGameMode(enabled);
    }

    private void applyTouchGameMode(boolean enabled) {
        ITouchFeature touchFeature = getTouchFeature();
        if (touchFeature == null) {
            return;
        }
        try {
            touchFeature.setTouchMode(TOUCH_ID, MODE_GAME, enabled ? 1 : 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set the touch game mode", e);
            mTouchFeature = null;
        }
    }

    boolean isReadingMode() {
        return getPrefs().getBoolean(KEY_READING_MODE, false);
    }

    void setReadingMode(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_READING_MODE, enabled).apply();
        setDisplayFeature(FEATURE_PAPER_MODE, enabled ? 1 : 0);
    }

    boolean isTrueTone() {
        return getPrefs().getBoolean(KEY_TRUE_TONE, false);
    }

    void setTrueTone(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_TRUE_TONE, enabled).apply();
        setDisplayFeature(FEATURE_TRUE_TONE, enabled ? 1 : 0);
    }

    boolean isSunlightMode() {
        return getPrefs().getBoolean(KEY_SUNLIGHT_MODE, false);
    }

    void setSunlightMode(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_SUNLIGHT_MODE, enabled).apply();
        mSunlightModeController.setEnabled(enabled);
    }

    void setDisplayFeature(int feature, int value) {
        IBinder binder = ServiceManager.waitForDeclaredService(DISPLAY_FEATURE_SERVICE);
        if (binder == null) {
            Log.e(TAG, "DisplayFeature service is not available");
            return;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DISPLAY_FEATURE_DESCRIPTOR);
            data.writeInt(Display.DEFAULT_DISPLAY);
            data.writeInt(feature);
            data.writeInt(value);
            data.writeInt(255);
            binder.transact(TRANSACTION_SET_FEATURE, data, reply, 0);
            reply.readException();
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Failed to set display feature " + feature, e);
        } finally {
            data.recycle();
            reply.recycle();
        }
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
