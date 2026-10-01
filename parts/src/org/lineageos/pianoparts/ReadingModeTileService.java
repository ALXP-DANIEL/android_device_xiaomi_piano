/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Toggles the DisplayFeature paper (reading) mode, as the HyperOS reading
 * mode tile does.
 */
public class ReadingModeTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        PianoPartsApp app = (PianoPartsApp) getApplication();
        app.setReadingMode(!app.isReadingMode());
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        boolean enabled = ((PianoPartsApp) getApplication()).isReadingMode();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
