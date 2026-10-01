/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UEventObserver;
import android.util.Log;

import java.util.Locale;

/**
 * Like HyperOS, shows the stylus battery popup when the pen snaps onto the
 * magnetic charger and when it runs low, and keeps the level in a quiet
 * notification while the pen is connected.
 *
 * The level comes from the charger's power_supply uevent
 * (POWER_SUPPLY_REVERSE_PEN_SOC, as stock miui-services reads it) or from the
 * pen's Bluetooth battery report.
 *
 * Testing without a pen:
 *   adb shell am broadcast -a org.lineageos.pianoparts.action.TEST_PEN_POPUP \
 *       --ei level 80 --ez charging true
 */
class PenBatteryNotifier extends BroadcastReceiver {

    private static final String TAG = "PianoPartsPen";

    // BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED and EXTRA_BATTERY_LEVEL.
    private static final String ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";
    private static final String EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL";
    private static final String ACTION_TEST =
            "org.lineageos.pianoparts.action.TEST_PEN_POPUP";

    private static final String UEVENT_MATCH = "SUBSYSTEM=power_supply";
    private static final String UEVENT_PEN_SOC = "POWER_SUPPLY_REVERSE_PEN_SOC";
    private static final String UEVENT_PEN_CHG_STATE = "POWER_SUPPLY_REVERSE_PEN_CHG_STATE";

    private static final String CHANNEL_ID = "pen_battery";
    private static final int NOTIFICATION_ID = 1;
    private static final int LOW_LEVEL = 20;

    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final PenPopup mPopup;
    private int mLastLevel = -1;
    private int mLastChargeState = -1;

    private final UEventObserver mUEventObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            String soc = event.get(UEVENT_PEN_SOC);
            if (soc == null) {
                return;
            }
            int chargeState = parseInt(event.get(UEVENT_PEN_CHG_STATE), 0);
            int level = parseInt(soc, -1);
            // The popup appears when the pen starts charging on the magnet.
            boolean attached = chargeState > 0 && mLastChargeState <= 0;
            mLastChargeState = chargeState;
            onLevel(level, chargeState > 0, attached);
        }
    };

    PenBatteryNotifier(Context context) {
        mContext = context;
        mPopup = new PenPopup(context);
        mNotificationManager = context.getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.pen_battery_channel),
                NotificationManager.IMPORTANCE_LOW));

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BATTERY_LEVEL_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED);

        // Only the shell (which holds DUMP) can send the test broadcast.
        context.registerReceiver(this, new IntentFilter(ACTION_TEST),
                android.Manifest.permission.DUMP, null, Context.RECEIVER_EXPORTED);

        mUEventObserver.startObserving(UEVENT_MATCH);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_TEST.equals(action)) {
            mPopup.show(intent.getIntExtra("level", 80),
                    intent.getBooleanExtra("charging", true));
            return;
        }
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice.class);
        if (!isPen(device)) {
            return;
        }
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            mLastLevel = -1;
            mNotificationManager.cancel(NOTIFICATION_ID);
            return;
        }
        int level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1);
        onLevel(level, false, mLastLevel < 0);
    }

    private void onLevel(int level, boolean charging, boolean attached) {
        if (level < 0 || level > 100) {
            return;
        }
        boolean wentLow = level <= LOW_LEVEL && mLastLevel > LOW_LEVEL;
        mLastLevel = level;
        if (attached || wentLow) {
            mPopup.show(level, charging);
        }
        Notification notification = new Notification.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pen)
                .setContentTitle(mContext.getString(R.string.pen_battery_title))
                .setContentText(mContext.getString(R.string.pen_battery_level, level))
                .setProgress(100, level, false)
                .setOnlyAlertOnce(true)
                .build();
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private boolean isPen(BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        try {
            String name = device.getName();
            return name != null && name.toLowerCase(Locale.ROOT).contains("pen");
        } catch (SecurityException e) {
            Log.e(TAG, "Cannot read the Bluetooth device name", e);
            return false;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
