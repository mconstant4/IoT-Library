package edu.uri.wbl.wbl_library.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * Created by mconstant on 12/6/17.
 */

public class BleActionModel {
    private BleAction mBleAction;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;
    private byte[] mValue;

    public BleActionModel(BleAction action, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        mBleAction = action;
        mBluetoothGatt = gatt;
        mCharacteristic = characteristic;
        mValue = value;
    }

    public BleActionModel(BleAction action, String bluetoothDeviceAddress) {
        mBleAction = action;
        mBluetoothDeviceAddress = bluetoothDeviceAddress;
    }

    public BleActionModel(BleAction action, BluetoothGatt gatt) {
        mBleAction = action;
        mBluetoothGatt = gatt;
    }

    public BleActionModel(BleAction action, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        mBleAction = action;
        mBluetoothGatt = gatt;
        mCharacteristic = characteristic;
    }

    public BleAction getBleAction() {
        return mBleAction;
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

    public void setValue(byte[] value) {
        mValue = value;
    }
}
