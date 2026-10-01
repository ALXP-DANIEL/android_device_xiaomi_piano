/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.InputDevice;

/**
 * Talks to the pogo pin keyboard the way stock miui-services does
 * (padkeyboard/iic): 68 byte packets sent through the keyboard nanoapp HAL.
 *
 * Packet: AA 42 32 00 | 4E 31 80 38 <command> 01 <value> <sum of bytes 4..10>
 */
final class PadKeyboard {

    private static final String TAG = "PianoPartsKeyboard";

    private static final String SERVICE =
            "vendor.xiaomi.hardware.keyboardnanoapp_aidl.IKeyboardNanoapp_aidl/default";
    private static final String DESCRIPTOR =
            "vendor.xiaomi.hardware.keyboardnanoapp_aidl.IKeyboardNanoapp_aidl";
    private static final int TRANSACTION_SEND_CMD = IBinder.FIRST_CALL_TRANSACTION;

    // The keyboard cover's input device (see configs/idc).
    private static final int KEYBOARD_VENDOR_ID = 0x15d9;
    private static final int KEYBOARD_PRODUCT_ID = 0x00a3;

    // IICCommandMaker feature commands.
    static final byte COMMAND_TOUCHPAD_ENABLE = 33;
    static final byte COMMAND_BACKLIGHT = 35;

    private static final int PACKET_SIZE = 68;

    private PadKeyboard() {}

    static boolean isConnected(Context context) {
        InputManager inputManager = context.getSystemService(InputManager.class);
        for (int id : inputManager.getInputDeviceIds()) {
            InputDevice device = inputManager.getInputDevice(id);
            if (device != null && device.getVendorId() == KEYBOARD_VENDOR_ID
                    && device.getProductId() == KEYBOARD_PRODUCT_ID) {
                return true;
            }
        }
        return false;
    }

    static boolean setFeature(byte command, int value) {
        byte[] packet = new byte[PACKET_SIZE];
        // IICCommandMaker.setCommandHead
        packet[0] = (byte) 0xAA;
        packet[1] = 0x42;
        packet[2] = 0x32;
        packet[3] = 0x00;
        // IICCommandMaker.setSetKeyboardStatusCommand
        packet[4] = 0x4E;
        packet[5] = 0x31;
        packet[6] = (byte) 0x80;
        packet[7] = 0x38;
        packet[8] = command;
        packet[9] = 0x01;
        packet[10] = (byte) value;
        byte sum = 0;
        for (int i = 4; i <= 10; i++) {
            sum += packet[i];
        }
        packet[11] = sum;
        return send(packet);
    }

    private static boolean send(byte[] packet) {
        IBinder binder = ServiceManager.checkService(SERVICE);
        if (binder == null) {
            Log.e(TAG, "Keyboard nanoapp service is not available");
            return false;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeByteArray(packet);
            binder.transact(TRANSACTION_SEND_CMD, data, reply, 0);
            reply.readException();
            return reply.readInt() >= 0;
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Failed to send a keyboard command", e);
            return false;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
