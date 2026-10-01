/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Toggles the touch panel game mode (faster touch reporting), as the HyperOS
 * game turbo does.
 */
public class TouchGameModeTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        PianoPartsApp app = (PianoPartsApp) getApplication();
        app.setTouchGameMode(!app.isTouchGameMode());
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        boolean enabled = ((PianoPartsApp) getApplication()).isTouchGameMode();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
