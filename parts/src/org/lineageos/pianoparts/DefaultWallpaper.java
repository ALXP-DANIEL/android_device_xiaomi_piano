/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Like HyperOS, sets the stock wallpaper that matches the tablet's body colour
 * once, on the first boot.
 */
class DefaultWallpaper {

    private static final String TAG = "PianoPartsWallpaper";
    private static final String PROP_BODY_COLOR = "ro.vendor.piano.body_color";
    private static final String WALLPAPER_DIR = "/product/media/wallpaper/";
    private static final String DEFAULT_COLOR = "BU";
    private static final String KEY_DONE = "default_wallpaper_set";

    static void applyOnce(Context context, SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_DONE, false)) {
            return;
        }
        if (context.getSystemService(UserManager.class).isUserUnlocked()) {
            apply(context, prefs);
            return;
        }
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                c.unregisterReceiver(this);
                apply(c, prefs);
            }
        }, new IntentFilter(Intent.ACTION_USER_UNLOCKED), Context.RECEIVER_NOT_EXPORTED);
    }

    private static void apply(Context context, SharedPreferences prefs) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        // Leave a wallpaper the user already picked alone.
        try (android.os.ParcelFileDescriptor current =
                wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)) {
            if (current != null || wallpaperManager.getWallpaperInfo() != null) {
                prefs.edit().putBoolean(KEY_DONE, true).apply();
                return;
            }
        } catch (IOException e) {
            // Treat as no custom wallpaper.
        }
        String color = SystemProperties.get(PROP_BODY_COLOR, DEFAULT_COLOR);
        File file = new File(WALLPAPER_DIR + "wallpaper_" + color + ".jpg");
        if (!file.exists()) {
            file = new File(WALLPAPER_DIR + "wallpaper_" + DEFAULT_COLOR + ".jpg");
        }
        try (InputStream in = new FileInputStream(file)) {
            wallpaperManager.setStream(in, null, true,
                    WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            prefs.edit().putBoolean(KEY_DONE, true).apply();
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Failed to set the default wallpaper " + file, e);
        }
    }
}
