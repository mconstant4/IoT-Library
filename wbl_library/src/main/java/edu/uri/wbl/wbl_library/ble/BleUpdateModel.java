package edu.uri.wbl.wbl_library.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.io.Serializable;

/**
 * Created by mconstant on 12/7/17.
 */

public class BleUpdateModel implements Serializable {
    private BleUpdate mBleUpdate;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;
    private byte[] mValue;

    public BleUpdateModel(BleUpdate bleUpdate, String bluetoothDeviceAddress) {
        mBleUpdate = bleUpdate;
        mBluetoothDeviceAddress = bluetoothDeviceAddress;
    }

    public BleUpdateModel(BleUpdate bleUpdate, BluetoothGatt gatt) {
        mBleUpdate = bleUpdate;
        mBluetoothGatt = gatt;
    }

    public BleUpdateModel(BleUpdate bleUpdate, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        mBleUpdate = bleUpdate;
        mBluetoothGatt = gatt;
        mCharacteristic = characteristic;
        mValue = value;
    }

    public String getBluetoothDeviceAddress() {
        return mBluetoothDeviceAddress;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

    public byte[] getValue() {
        return mValue;
    }
}
