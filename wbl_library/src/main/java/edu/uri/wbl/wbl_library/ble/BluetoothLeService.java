package edu.uri.wbl.wbl_library.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.UUID;

import static edu.uri.wbl.wbl_library.ble.BleAction.CONNECT;

/**
 * Created by mconstant on 12/6/17.
 */

public class BluetoothLeService extends Service {
    public static void CONNECT(Context context, String bd_addr) {
        if(bd_addr == null) {
            Log.e(TAG, "Error: BD_ADDR NULL");
            return;
        }

        Intent intent = new Intent(context, BluetoothLeService.class);
        intent.putExtra(EXTRA_ACTION, CONNECT);
        intent.putExtra(EXTRA_BD_ADDR, bd_addr);
        context.startService(intent);
    }

    private static final int RETURN_POLICY = START_STICKY;
    private static final String TAG = "BluetoothLeService";
    private static final String EXTRA_ACTION = "wbl.library.ble.extras.action";
    private static final String EXTRA_BD_ADDR = "wbl.library.ble.extras.bd_addr";
    private static final String EXTRA_GATT = "wbl.library.ble.extras.gatt";
    private static final String EXTRA_CHARACTERISTIC = "wbl.library.ble.extras.char";
    private static final String EXTRA_VALUE = "wbl.library.ble.extras.value";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            BleActionModel bleActionModel = (BleActionModel) message.obj;
            if(bleActionModel == null) {
                Log.w(TAG, "Error Receiving BLE Action Model");
                return;
            }

            BleAction bleAction = bleActionModel.getBleAction();
            switch (bleAction) {
                case CONNECT:
                    String bd_addr = bleActionModel.getBluetoothDeviceAddress();
                    if(bd_addr == null) {
                        Log.w(TAG, "Received NULL BD_ADDR");
                        break;
                    }

                    log("Action: CONNECT:\n\tBD_ADDR: " + bd_addr);
                    break;
                default:

                    break;
            }
        }
    }

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, BluetoothGatt> mConnectedDevices;

    @Override
    public void onCreate() {
        super.onCreate();

        log("Service Created");

        mContext = this;                            // Store reference to Service's Context

        HandlerThread thread = new HandlerThread(   // Create Thread to Handle BLE Actions
                "BleActionThread",
                Process.THREAD_PRIORITY_BACKGROUND
        );
        thread.start();                             // Start BLE Action Thread

        // Initialize Handler and Looper (needed by BLE Action Thread)
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        // Initialize Bluetooth Adapter (required for all Bluetooth-related API calls)
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if(bluetoothManager == null) {
            Log.e(TAG, "FATAL: Could not Obtain Bluetooth Manager");
            stopSelf();
            return;
        }
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter == null) {
            Log.e(TAG, "FATAL: Could not Obtain Bluetooth Adapter");
            stopSelf();
            return;
        }

        // Initialize Hash Map to Store all Currently Connected Devices
        mConnectedDevices = new HashMap<>(7);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null || !intent.hasExtra(EXTRA_ACTION)) {
            Log.e(TAG, "Error: Invalid Action Packet");
            return RETURN_POLICY;
        }

        Message message = mServiceHandler.obtainMessage();
        BleActionModel bleActionModel;
        BleAction action = (BleAction) intent.getSerializableExtra(EXTRA_ACTION);

        switch (action) {
            case CONNECT:
                /**
                 * Valid CONNECT Action Packet:
                 *      BleAction {
                 *          BleAction = CONNECT
                 *          BD_ADDR = <valid Bluetooth Device Address>
                 *      }
                 */

                if(!intent.hasExtra(EXTRA_BD_ADDR)) {
                    Log.w(TAG, "Invalid CONNECT Action Packet Received");
                    return RETURN_POLICY;
                }

                bleActionModel = new BleActionModel(action, intent.getStringExtra(EXTRA_BD_ADDR));
                message.obj = bleActionModel;

                break;
            case DISCOVER_SERVICES:

                break;
            case READ_CHARACTERISTIC:

                break;
            case WRITE_CHARACTERISTIC:

                break;
            case ENABLE_NOTIFICATION:

                break;
            case DISABLE_NOTIFICATION:

                break;
            case DISCONNECT:

                break;
            default:
                Log.w(TAG, "Unkown Action Sent");
                break;
        }

        return RETURN_POLICY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        log("Service Destroyed");

        super.onDestroy();
    }

    private void connect(String bluetoothDeviceAddress) {
        if(bluetoothDeviceAddress == null || !BluetoothAdapter.checkBluetoothAddress(bluetoothDeviceAddress)) {
            Log.e(TAG, "Error: BD_ADDR is Invalid");
            return;
        }

        if(mConnectedDevices.containsKey(bluetoothDeviceAddress)) {
            Log.w(TAG, "Requested Device already Connected");
            return;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(bluetoothDeviceAddress);
        device.connectGatt(mContext, false, mBluetoothGattCallback);

        log("Connecting to " + bluetoothDeviceAddress);
        Toast.makeText(mContext, "Coonecting to " + bluetoothDeviceAddress, Toast.LENGTH_LONG).show();
    }

    private void log(String message) {
        Log.d(TAG, message);
    }

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };
}
