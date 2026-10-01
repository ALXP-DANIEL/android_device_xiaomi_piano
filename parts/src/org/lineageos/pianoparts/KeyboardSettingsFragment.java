/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

public class KeyboardSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, InputManager.InputDeviceListener {

    private KeyboardController mController;
    private Preference mNotConnected;
    private SwitchPreferenceCompat mAuto;
    private SeekBarPreference mBrightness;
    private SwitchPreferenceCompat mTouchpad;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setStorageDeviceProtected();
        getPreferenceManager().setSharedPreferencesName(KeyboardController.PREFS_NAME);
        setPreferencesFromResource(R.xml.keyboard_settings, rootKey);
        mController = ((PianoPartsApp) requireContext().getApplicationContext())
                .getKeyboardController();
        mNotConnected = findPreference("keyboard_not_connected");
        mAuto = findPreference(KeyboardController.KEY_BACKLIGHT_AUTO);
        mBrightness = findPreference(KeyboardController.KEY_BACKLIGHT_LEVEL);
        mTouchpad = findPreference(KeyboardController.KEY_TOUCHPAD);
        mAuto.setOnPreferenceChangeListener(this);
        mBrightness.setOnPreferenceChangeListener(this);
        mTouchpad.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireContext().getSystemService(InputManager.class)
                .registerInputDeviceListener(this, new Handler(Looper.getMainLooper()));
        updateState();
    }

    @Override
    public void onPause() {
        requireContext().getSystemService(InputManager.class).unregisterInputDeviceListener(this);
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Let the preference store the value first, then apply it.
        new Handler(Looper.getMainLooper()).post(() -> {
            mController.apply();
            updateState();
        });
        return true;
    }

    private void updateState() {
        boolean connected = PadKeyboard.isConnected(requireContext());
        mNotConnected.setVisible(!connected);
        mAuto.setEnabled(connected);
        mBrightness.setEnabled(connected && !mAuto.isChecked());
        mTouchpad.setEnabled(connected);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateState();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateState();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateState();
    }
}
