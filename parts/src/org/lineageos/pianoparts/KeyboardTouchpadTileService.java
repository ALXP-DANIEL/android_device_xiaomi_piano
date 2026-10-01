/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.content.Context;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Turns the pogo pin keyboard's touchpad on or off.
 */
public class KeyboardTouchpadTileService extends TileService {

    private static final String KEY_ENABLED = "keyboard_touchpad";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!PadKeyboard.isConnected(this)) {
            updateTile();
            return;
        }
        boolean enabled = !isEnabled();
        if (PadKeyboard.setFeature(PadKeyboard.COMMAND_TOUCHPAD_ENABLE, enabled ? 1 : 0)) {
            prefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
        }
        updateTile();
    }

    private boolean isEnabled() {
        return prefs().getBoolean(KEY_ENABLED, true);
    }

    private SharedPreferences prefs() {
        return createDeviceProtectedStorageContext()
                .getSharedPreferences("piano_parts", Context.MODE_PRIVATE);
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        if (!PadKeyboard.isConnected(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setSubtitle(getString(R.string.keyboard_not_connected));
        } else {
            tile.setState(isEnabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setSubtitle(null);
        }
        tile.updateTile();
    }
}
