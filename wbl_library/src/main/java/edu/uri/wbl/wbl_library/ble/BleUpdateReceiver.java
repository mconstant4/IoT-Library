package edu.uri.wbl.wbl_library.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by mconstant on 12/7/17.
 */

public abstract class BleUpdateReceiver extends BroadcastReceiver {
    public static final IntentFilter INTENT_FILTER = new IntentFilter("wbl.library.ble.update.filter");
    public static final String EXTRA_UPDATE_MODEL = "wbl.library.ble.update.model";

    public static void UPDATE_CONNECTED(Context context, String bluetoothDeviceAddress) {
        BleUpdateModel updateModel = new BleUpdateModel(BleUpdate.CONNECTED, bluetoothDeviceAddress);
        UPDATE(context, updateModel);
    }

    public static void UPDATE_DISCONNECTED(Context context, String bluetoothDeviceAddress) {
        BleUpdateModel updateModel = new BleUpdateModel(BleUpdate.DISCONNECTED, bluetoothDeviceAddress);
        UPDATE(context, updateModel);
    }

    public static void UPDATE_SERVICES_DISCOVERED(Context context, BluetoothGatt gatt) {
        BleUpdateModel updateModel = new BleUpdateModel(BleUpdate.SERVICES_DISCOVERED, gatt);
        UPDATE(context, updateModel);

    }

    public static void UPDATE_CHARACTERISTIC_READ(Context context, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        BleUpdateModel updateModel = new BleUpdateModel(BleUpdate.CHARACTERISTIC_READ, gatt, characteristic, value);
        UPDATE(context, updateModel);
    }

    public static void UPDATE_CHARACTERISTIC_WRITTEN(Context context, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        BleUpdateModel updateModel = new BleUpdateModel(BleUpdate.CHARACTERISTIC_WRITTEN, gatt, characteristic, value);
        UPDATE(context, updateModel);
    }

    public static void UPDATE_CHARACTERISTIC_NOTIFIED(Context context, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        BleUpdateModel updateModel = new BleUpdateModel(BleUpdate.CHARACTERISTIC_NOTIFIED, gatt, characteristic, value);
        UPDATE(context, updateModel);
    }

    private static void UPDATE(Context context, BleUpdateModel bleUpdateModel) {
        Intent intent = new Intent(INTENT_FILTER.getAction(0));
        intent.putExtra(EXTRA_UPDATE_MODEL, bleUpdateModel);
        context.sendBroadcast(intent);
    }
}
