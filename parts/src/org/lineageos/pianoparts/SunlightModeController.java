/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;

/**
 * Like HyperOS, raises the DisplayFeature sunlight screen level in bright
 * ambient light so the panel stays readable outdoors.
 */
class SunlightModeController implements SensorEventListener {

    // Hysteresis so the boost does not flicker around a single threshold.
    private static final float LUX_ON = 5000f;
    private static final float LUX_OFF = 3000f;
    private static final int LEVEL_ON = 5;
    private static final int LEVEL_OFF = 0;

    private final PianoPartsApp mApp;
    private final SensorManager mSensorManager;
    private final Sensor mLightSensor;

    private boolean mEnabled;
    private boolean mListening;
    private boolean mBoosted;

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateListening();
        }
    };

    SunlightModeController(PianoPartsApp app) {
        mApp = app;
        mSensorManager = app.getSystemService(SensorManager.class);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        app.registerReceiver(mScreenReceiver, filter);
    }

    void setEnabled(boolean enabled) {
        mEnabled = enabled;
        updateListening();
    }

    private void updateListening() {
        boolean screenOn = mApp.getSystemService(PowerManager.class).isInteractive();
        boolean listen = mEnabled && screenOn && mLightSensor != null;
        if (listen == mListening) {
            return;
        }
        mListening = listen;
        if (listen) {
            mSensorManager.registerListener(this, mLightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mSensorManager.unregisterListener(this);
            setBoosted(false);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float lux = event.values[0];
        if (!mBoosted && lux >= LUX_ON) {
            setBoosted(true);
        } else if (mBoosted && lux < LUX_OFF) {
            setBoosted(false);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void setBoosted(boolean boosted) {
        if (boosted == mBoosted) {
            return;
        }
        mBoosted = boosted;
        mApp.setDisplayFeature(PianoPartsApp.FEATURE_SUNLIGHT_SCREEN,
                boosted ? LEVEL_ON : LEVEL_OFF);
    }
}
