/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.content.Context;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Cycles the pogo pin keyboard backlight: off, half, full.
 */
public class KeyboardBacklightTileService extends TileService {

    private static final String KEY_LEVEL = "keyboard_backlight";
    private static final int[] LEVELS = {0, 50, 100};

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
        int index = (getIndex() + 1) % LEVELS.length;
        if (PadKeyboard.setFeature(PadKeyboard.COMMAND_BACKLIGHT, LEVELS[index])) {
            prefs().edit().putInt(KEY_LEVEL, index).apply();
        }
        updateTile();
    }

    private int getIndex() {
        return Math.min(prefs().getInt(KEY_LEVEL, 0), LEVELS.length - 1);
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
            int level = LEVELS[getIndex()];
            tile.setState(level > 0 ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setSubtitle(level > 0 ? level + "%" : getString(R.string.keyboard_off));
        }
        tile.updateTile();
    }
}
