/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * Applies the keyboard cover settings when it is attached, and like stock
 * follows the screen brightness (and turns the backlight off with the screen)
 * while the automatic backlight is on.
 */
class KeyboardController implements InputManager.InputDeviceListener {

    static final String PREFS_NAME = "piano_parts";
    static final String KEY_BACKLIGHT_AUTO = "keyboard_backlight_auto";
    static final String KEY_BACKLIGHT_LEVEL = "keyboard_backlight_level";
    static final String KEY_TOUCHPAD = "keyboard_touchpad";

    private static final int MAX_SCREEN_BRIGHTNESS = 255;

    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mConnected;
    private int mLastBacklight = -1;
    private Boolean mLastTouchpad;

    private final ContentObserver mBrightnessObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            applyBacklight();
        }
    };

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            applyBacklight();
        }
    };

    KeyboardController(Context context) {
        mContext = context;
        context.getSystemService(InputManager.class).registerInputDeviceListener(this, mHandler);
        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false,
                mBrightnessObserver);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(mScreenReceiver, filter);
        updateConnected();
    }

    /** Applies the stored settings now. */
    void apply() {
        mLastBacklight = -1;
        mLastTouchpad = null;
        applyBacklight();
        applyTouchpad();
    }

    private SharedPreferences prefs() {
        return mContext.createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void applyBacklight() {
        if (!mConnected) {
            return;
        }
        SharedPreferences prefs = prefs();
        int level;
        if (!mContext.getSystemService(PowerManager.class).isInteractive()) {
            level = 0;
        } else if (prefs.getBoolean(KEY_BACKLIGHT_AUTO, true)) {
            int screen = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, MAX_SCREEN_BRIGHTNESS / 2);
            level = Math.round(screen * 100f / MAX_SCREEN_BRIGHTNESS);
        } else {
            level = prefs.getInt(KEY_BACKLIGHT_LEVEL, 50);
        }
        level = Math.max(0, Math.min(100, level));
        if (level != mLastBacklight
                && PadKeyboard.setFeature(PadKeyboard.COMMAND_BACKLIGHT, level)) {
            mLastBacklight = level;
        }
    }

    private void applyTouchpad() {
        if (!mConnected) {
            return;
        }
        boolean enabled = prefs().getBoolean(KEY_TOUCHPAD, true);
        if (!Boolean.valueOf(enabled).equals(mLastTouchpad)
                && PadKeyboard.setFeature(PadKeyboard.COMMAND_TOUCHPAD_ENABLE, enabled ? 1 : 0)) {
            mLastTouchpad = enabled;
        }
    }

    private void updateConnected() {
        boolean connected = PadKeyboard.isConnected(mContext);
        if (connected == mConnected) {
            return;
        }
        mConnected = connected;
        if (connected) {
            // The keyboard comes up with its own defaults: send ours again.
            apply();
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateConnected();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateConnected();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateConnected();
    }
}
