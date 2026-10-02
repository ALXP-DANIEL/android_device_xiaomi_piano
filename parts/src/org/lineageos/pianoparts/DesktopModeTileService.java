/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Switches Android desktop windowing (floating, resizable app windows), the
 * counterpart of the HyperOS desktop mode tile. It is the same switch as the
 * "Enable desktop experience features" developer option and applies after a
 * restart.
 */
public class DesktopModeTileService extends TileService {

    private static final String DESKTOP_EXPERIENCE_PROPERTY =
            "persist.wm.debug.desktop_experience_devopts";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile(false);
    }

    @Override
    public void onClick() {
        super.onClick();
        SystemProperties.set(DESKTOP_EXPERIENCE_PROPERTY, isEnabled() ? "0" : "1");
        updateTile(true);
        showDialog(new AlertDialog.Builder(this)
                .setTitle(R.string.desktop_mode_restart_title)
                .setMessage(R.string.desktop_mode_restart_message)
                .setNegativeButton(R.string.desktop_mode_restart_later, null)
                .setPositiveButton(R.string.desktop_mode_restart_now,
                        (DialogInterface dialog, int which) ->
                                getSystemService(PowerManager.class).reboot("desktop mode"))
                .create());
    }

    private static boolean isEnabled() {
        return SystemProperties.getBoolean(DESKTOP_EXPERIENCE_PROPERTY, false);
    }

    private void updateTile(boolean pendingRestart) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(isEnabled() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setSubtitle(pendingRestart ? getString(R.string.desktop_mode_restart_subtitle) : null);
        tile.updateTile();
    }
}
