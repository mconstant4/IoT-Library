package edu.uri.wbl.wbl_library.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
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
import static edu.uri.wbl.wbl_library.ble.BleAction.DISABLE_NOTIFICATION;
import static edu.uri.wbl.wbl_library.ble.BleAction.DISCOVER_SERVICES;
import static edu.uri.wbl.wbl_library.ble.BleAction.ENABLE_NOTIFICATION;
import static edu.uri.wbl.wbl_library.ble.BleAction.READ_CHARACTERISTIC;
import static edu.uri.wbl.wbl_library.ble.BleAction.WRITE_CHARACTERISTIC;

/**
 * The BluetoothLeService is responsible for providing a layer of abstraction between Android's
 * BLE API and the user (in this case, the developer using the WBL IoT Library). This Service
 * provides all of the essential BLE functions including connecting to and disconnecting from
 * nearby BLE devices, discovering services available on connected BLE devices, reading and
 * writing characteristics, and enabling notifications for when a characteristic's value is changed.
 * The main goal of this Service (and all of its related components) is to allow developers to
 * quickly and efficiently develop Android IoT applications without having to deal with the
 * nuances associated with Android's BLE API.
 *
 * This Service is structured in such a way that the user interacts with it via "Action Packets".
 * Static methods are provided so that the user does not need to worry about the internals of the
 * "Action Packets" and just calls the appropriate method to carry out the desired action. For
 * example, in order for the user to attempt to connect to a nearby BLE device, he/she simply
 * calls the BluetoothLeService.CONNECT(Context, String) method supplying the Bluetooth Device
 * Address of the desired BLE device in String format. This Service does not provide a scanning
 * function, meaning the user must acquire the device's Bluetooth Device Address somehow (either
 * statically or dynamically) before this Service will be useful to him/her.
 *
 * Information and data is sent back to the caller via "Update Packets". These packets are delivered
 * to any component with a registered BleUpdateReceiver. Updates include connection state changes,
 * services discovered, and characteristics read/written/notified. View the BleDemoActivity for
 * a sample implementation of a BleUpdateReceiver.
 *
 * @author Matthew Constant
 * @version 1.0, 12/07/2017
 */

public class BluetoothLeService extends Service {
    /**
     * The return value for the onStartCommand method. START_STICKY means that if the OS kills this
     * Service to free up resources, it will restart this Service once it is possible to do so.
     */
    private static final int RETURN_POLICY = START_STICKY;
    /**
     * The tag used for all debug logging.
     */
    private static final String TAG = "BluetoothLeService";
    /**
     * The key for all Intent extras containing the BleAction to be taken.
     */
    private static final String EXTRA_ACTION = "wbl.library.ble.extras.action";
    /**
     * The key for all Intent extras containing the Bluetooth Device Address.
     */
    private static final String EXTRA_BD_ADDR = "wbl.library.ble.extras.bd_addr";
    /**
     * The key for all Intent extras containing the Bluetooth GATT.
     */
    private static final String EXTRA_GATT = "wbl.library.ble.extras.gatt";
    /**
     * The key for all Intent extras containing the Service UUID (as a String)
     */
    private static final String EXTRA_SERVICE = "wbl.library.ble.extras.service";
    /**
     * The key for all Intent extras containing the Characteristic UUID (as a String)
     */
    private static final String EXTRA_CHARACTERISTIC = "wbl.library.ble.extras.char";
    /**
     * The key for all Intent extras containing the Characteristic's value.
     */
    private static final String EXTRA_VALUE = "wbl.library.ble.extras.value";
    /**
     * The UUID (as a String) of the descriptor controlling whether the Characteristic has been
     * enabled for notifications or not.
     */
    private static final String NOTIFICATION_DESCRIPTOR = "00002902-0000-1000-8000-00805F9B34FB";

    /**
     * The CONNECT action attempts to connect to the BLE Device corresponding to the Bluetooth
     * Device Address provided. Upon success, the UPDATE_CONNECT update packet is broadcast.
     *
     * @param context Context of the calling component
     * @param bd_addr The Bluetooth Device Address of the desired BLE Device
     */
    public static void CONNECT(Context context, String bd_addr) {
        if(bd_addr == null) {
            Log.e(TAG, "Error: BD_ADDR NULL");
            return;
        }

        // Populate Connect Action Packet
        Intent intent = new Intent(context, BluetoothLeService.class);
        intent.putExtra(EXTRA_ACTION, CONNECT);
        intent.putExtra(EXTRA_BD_ADDR, bd_addr);
        context.startService(intent);
    }

    /**
     * The DISCOVER_SERVICES action attempts to query and store all Services available on the
     * connected BLE Device. Upon success, the SERVICES_DISCOVERED update packet is broadcast.
     *
     * NOTE: The BLE Device referenced must have already been connected via a call to this Service.
     *
     * @param context Context of the calling component
     * @param bd_addr The Bluetooth Device Address of the desired BLE Device
     */
    public static void DISCOVER_SERVICES(Context context, String bd_addr) {
        if(bd_addr == null) {
            Log.e(TAG, "Error: BD_ADDR NULL");
            return;
        }

        Intent intent = new Intent(context, BluetoothLeService.class);
        intent.putExtra(EXTRA_ACTION, DISCOVER_SERVICES);
        intent.putExtra(EXTRA_BD_ADDR, bd_addr);
        context.startService(intent);
    }

    /**
     * The READ_CHARACTERISTIC action sends a read request to the connected BLE Device. Upon success,
     * the UPDATE_CHARACTERISTIC_READ update packet is broadcast.
     *
     * NOTE: The BLE Device referenced must have already been connected via a call to this Service
     *          and the DISCOVERED_SERVICES update packet must have been successfully received.
     *
     * @param context Context of the calling component
     * @param bd_addr The Bluetooth Device Address of the desired BLE Device
     * @param serviceUuid Identifies the Service the Characteristic belongs to
     * @param charUuid Identifies the Characteristic to read from
     */
    public static void READ_CHARACTERISTIC(Context context, String bd_addr, String serviceUuid, String charUuid) {
        if(bd_addr == null || serviceUuid == null || charUuid == null) {
            Log.e(TAG, "Error: NULL Parameters");
            return;
        }

        Intent intent = new Intent(context, BluetoothLeService.class);
        intent.putExtra(EXTRA_ACTION, READ_CHARACTERISTIC);
        intent.putExtra(EXTRA_BD_ADDR, bd_addr);
        intent.putExtra(EXTRA_SERVICE, serviceUuid);
        intent.putExtra(EXTRA_CHARACTERISTIC, charUuid);
        context.startService(intent);
    }

    /**
     * The WRITE_CHARACTERISTIC action sends a read request to the connected BLE Device. Upon success,
     * the UPDATE_CHARACTERISTIC_WRITTEN update packet is broadcast.
     *
     * NOTE: The BLE Device referenced must have already been connected via a call to this Service
     *          and the DISCOVERED_SERVICES update packet must have been successfully received.
     *
     * @param context Context of the calling component
     * @param bd_addr The Bluetooth Device Address of the desired BLE Device
     * @param serviceUuid Identifies the Service the Characteristic belongs to
     * @param charUuid Identifies the Characteristic to write to
     */
    public static void WRITE_CHARACTERISTIC(Context context, String bd_addr, String serviceUuid, String charUuid, byte[] value) {
        if(bd_addr == null || serviceUuid == null || charUuid == null) {
            Log.e(TAG, "Error: NULL Parameters");
            return;
        }

        Intent intent = new Intent(context, BluetoothLeService.class);
        intent.putExtra(EXTRA_ACTION, WRITE_CHARACTERISTIC);
        intent.putExtra(EXTRA_BD_ADDR, bd_addr);
        intent.putExtra(EXTRA_SERVICE, serviceUuid);
        intent.putExtra(EXTRA_CHARACTERISTIC, charUuid);
        intent.putExtra(EXTRA_VALUE, value);
        context.startService(intent);
    }

    /**
     * The ENABLE_NOTIFICATION action attempts to enable notifications for the specified
     * Characteristic. Enabling notifications allows the user to be automatically notified any
     * time the Characteristic's value has changed.
     *
     * NOTE: The BLE Device referenced must have already been connected via a call to this Service
     *          and the DISCOVERED_SERVICES update packet must have been successfully received.
     *
     * @param context Context of the calling component
     * @param bd_addr The Bluetooth Device Address of the desired BLE Device
     * @param serviceUuid Identifies the Service the Characteristic belongs to
     * @param charUuid Identifies the Characteristic to be notified of
     */
    public static void ENABLE_NOTIFICATION(Context context, String bd_addr, String serviceUuid, String charUuid) {
        if(bd_addr == null || serviceUuid == null || charUuid == null) {
            Log.e(TAG, "Error: NULL Parameters");
            return;
        }

        Intent intent = new Intent(context, BluetoothLeService.class);
        intent.putExtra(EXTRA_ACTION, ENABLE_NOTIFICATION);
        intent.putExtra(EXTRA_BD_ADDR, bd_addr);
        intent.putExtra(EXTRA_SERVICE, serviceUuid);
        intent.putExtra(EXTRA_CHARACTERISTIC, charUuid);
        context.startService(intent);
    }

    /**
     * The DISABLE_NOTIFICATION action disables notifications for the specified
     * Characteristic.
     *
     * NOTE: The BLE Device referenced must have already been connected via a call to this Service
     *          and the DISCOVERED_SERVICES update packet must have been successfully received.
     *
     * @param context Context of the calling component
     * @param bd_addr The Bluetooth Device Address of the desired BLE Device
     * @param serviceUuid Identifies the Service the Characteristic belongs to
     * @param charUuid Identifies the Characteristic to disable notifications from
     */
    public static void DISABLE_NOTIFICATION(Context context, String bd_addr, String serviceUuid, String charUuid) {
        if(bd_addr == null || serviceUuid == null || charUuid == null) {
            Log.e(TAG, "Error: NULL Parameters");
            return;
        }

        Intent intent = new Intent(context, BluetoothLeService.class);
        intent.putExtra(EXTRA_ACTION, DISABLE_NOTIFICATION);
        intent.putExtra(EXTRA_BD_ADDR, bd_addr);
        intent.putExtra(EXTRA_SERVICE, serviceUuid);
        intent.putExtra(EXTRA_CHARACTERISTIC, charUuid);
        context.startService(intent);
    }

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    /**
     * This class runs on a separate Thread than the Service's (which is the main Thread). This
     * allows the Service to handle each action in the background rather than block the application's
     * main Thread.
     *
     * TODO: Explore possibility of creating new Thread for each action
     */
    private final class ServiceHandler extends Handler {
        private ServiceHandler(Looper looper) {
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
            BluetoothGatt gatt;
            BluetoothGattCharacteristic characteristic;
            switch (bleAction) {
                case CONNECT:
                    String bd_addr = bleActionModel.getBluetoothDeviceAddress();
                    if(bd_addr == null) {
                        Log.w(TAG, "Received NULL BD_ADDR");
                        break;
                    }

                    connect(bd_addr);
                    break;
                case DISCOVER_SERVICES:
                    gatt = bleActionModel.getBluetoothGatt();
                    if(gatt == null) {
                        Log.w(TAG, "Bluetooth GATT NULL");
                        break;
                    }

                    discoverServices(gatt);
                    break;
                case READ_CHARACTERISTIC:
                    gatt = bleActionModel.getBluetoothGatt();
                    if(gatt == null) {
                        Log.w(TAG, "Bluetooth GATT NULL");
                        break;
                    }

                    characteristic = bleActionModel.getCharacteristic();
                    if(characteristic == null) {
                        Log.w(TAG, "Characteristic NULL");
                        break;
                    }

                    readCharacteristic(gatt, characteristic);

                    break;
                case WRITE_CHARACTERISTIC:
                    gatt = bleActionModel.getBluetoothGatt();
                    if(gatt == null) {
                        Log.w(TAG, "Bluetooth GATT NULL");
                        break;
                    }

                    characteristic = bleActionModel.getCharacteristic();
                    if(characteristic == null) {
                        Log.w(TAG, "Characteristic NULL");
                        break;
                    }

                    byte[] value = bleActionModel.getValue();

                    writeCharacteristic(gatt, characteristic, value);

                    break;
                case ENABLE_NOTIFICATION:
                    gatt = bleActionModel.getBluetoothGatt();
                    if(gatt == null) {
                        Log.w(TAG, "Bluetooth GATT NULL");
                        break;
                    }

                    characteristic = bleActionModel.getCharacteristic();
                    if(characteristic == null) {
                        Log.w(TAG, "Characteristic NULL");
                        break;
                    }

                    enableNotification(gatt, characteristic);

                    break;
                case DISABLE_NOTIFICATION:
                    gatt = bleActionModel.getBluetoothGatt();
                    if(gatt == null) {
                        Log.w(TAG, "Bluetooth GATT NULL");
                        break;
                    }

                    characteristic = bleActionModel.getCharacteristic();
                    if(characteristic == null) {
                        Log.w(TAG, "Characteristic NULL");
                        break;
                    }

                    disableNotification(gatt, characteristic);

                    break;
                case DISCONNECT:
                    gatt = bleActionModel.getBluetoothGatt();
                    if(gatt == null) {
                        Log.w(TAG, "Bluetooth GATT NULL");
                        break;
                    }

                    disconnect(gatt);

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
        if(!intent.hasExtra(EXTRA_BD_ADDR)) {
            Log.w(TAG, "Invalid Action Packet Received");
            return RETURN_POLICY;
        }
        String bd_addr = intent.getStringExtra(EXTRA_BD_ADDR);
        if(bd_addr == null) {
            Log.w(TAG, "BD_ADDR NULL");
            return RETURN_POLICY;
        }
        BluetoothGatt gatt;

        switch (action) {
            case CONNECT:
                /**
                 * Valid CONNECT Action Packet:
                 *      BleAction {
                 *          BleAction = CONNECT
                 *          BD_ADDR = <valid Bluetooth Device Address>
                 *      }
                 */

                bleActionModel = new BleActionModel(action, intent.getStringExtra(EXTRA_BD_ADDR));
                message.obj = bleActionModel;

                break;
            case DISCOVER_SERVICES:
                /**
                 * Valid DISCOVER_SERVICES Action Packet
                 *      BleAction {
                 *          BleAction = DISCOVER_SERVICES
                 *          BluetoothGatt = <valid Bluetooth GATT>
                 *      }
                 */

                if(!mConnectedDevices.containsKey(bd_addr)) {
                    Log.w(TAG, "Device is  not Connected");
                    return RETURN_POLICY;
                }

                gatt = mConnectedDevices.get(bd_addr);
                if(gatt == null) {
                    Log.w(TAG, "GATT return NULL (Not Connected Properly)");
                    return RETURN_POLICY;
                }

                bleActionModel = new BleActionModel(action, gatt);
                message.obj = bleActionModel;

                break;
            case READ_CHARACTERISTIC:
                /**
                 * Valid READ_CHARACTERISTIC Action Packet
                 *      BleAction {
                 *          BleAction = READ_CHARACTERISTIC
                 *          Bluetooth GATT = <valid BluetoothGatt>
                 *          Characteristic = <valid BluetoothGattCharacteristic>
                 *      }
                 */

                if(!mConnectedDevices.containsKey(bd_addr)) {
                    Log.w(TAG, "Device is  not Connected");
                    return RETURN_POLICY;
                }

                gatt = mConnectedDevices.get(bd_addr);
                if(gatt == null) {
                    Log.w(TAG, "GATT return NULL (Not Connected Properly)");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_SERVICE)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                String readServiceUuid = intent.getStringExtra(EXTRA_SERVICE);

                BluetoothGattService readService = gatt.getService(UUID.fromString(readServiceUuid));
                if(readService == null) {
                    Log.w(TAG, "Could not Find Service");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_CHARACTERISTIC)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                String readCharacteristicUuid = intent.getStringExtra(EXTRA_CHARACTERISTIC);
                BluetoothGattCharacteristic readCharacteristic = readService.getCharacteristic(UUID.fromString(readCharacteristicUuid));
                if(readCharacteristic == null) {
                    Log.w(TAG, "Could not Find Characteristic");
                    return RETURN_POLICY;
                }

                bleActionModel = new BleActionModel(action, gatt, readCharacteristic);
                message.obj = bleActionModel;

                break;
            case WRITE_CHARACTERISTIC:
                /**
                 * Valid WRITE_CHARACTERISTIC Action Packet
                 *      BleAction {
                 *          BleAction = WRITE_CHARACTERISTIC
                 *          Bluetooth GATT = <valid BluetoothGatt>
                 *          Characteristic = <valid BluetoothGattCharacteristic>
                 *          Value = byte[]
                 *      }
                 */

                if(!mConnectedDevices.containsKey(bd_addr)) {
                    Log.w(TAG, "Device is  not Connected");
                    return RETURN_POLICY;
                }

                gatt = mConnectedDevices.get(bd_addr);
                if(gatt == null) {
                    Log.w(TAG, "GATT return NULL (Not Connected Properly)");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_SERVICE)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                String writeServiceUuid = intent.getStringExtra(EXTRA_SERVICE);

                BluetoothGattService writeService = gatt.getService(UUID.fromString(writeServiceUuid));
                if(writeService == null) {
                    Log.w(TAG, "Could not Find Service");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_CHARACTERISTIC)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                String writeCharacteristicUuid = intent.getStringExtra(EXTRA_CHARACTERISTIC);
                BluetoothGattCharacteristic writeCharacteristic = writeService.getCharacteristic(UUID.fromString(writeCharacteristicUuid));
                if(writeCharacteristic == null) {
                    Log.w(TAG, "Could not Find Characteristic");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_VALUE)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                byte[] writeValue = intent.getByteArrayExtra(EXTRA_VALUE);

                bleActionModel = new BleActionModel(action, gatt, writeCharacteristic, writeValue);
                message.obj = bleActionModel;

                break;
            case ENABLE_NOTIFICATION:
                /**
                 * Valid ENABLE_NOTIFICATION Action Packet
                 *      BleAction {
                 *          BleAction = READ_CHARACTERISTIC
                 *          Bluetooth GATT = <valid BluetoothGatt>
                 *          Characteristic = <valid BluetoothGattCharacteristic>
                 *      }
                 */

                if(!mConnectedDevices.containsKey(bd_addr)) {
                    Log.w(TAG, "Device is  not Connected");
                    return RETURN_POLICY;
                }

                gatt = mConnectedDevices.get(bd_addr);
                if(gatt == null) {
                    Log.w(TAG, "GATT return NULL (Not Connected Properly)");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_SERVICE)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                String enableServiceUuid = intent.getStringExtra(EXTRA_SERVICE);

                BluetoothGattService enableService = gatt.getService(UUID.fromString(enableServiceUuid));
                if(enableService == null) {
                    Log.w(TAG, "Could not Find Service");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_CHARACTERISTIC)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                String enableCharacteristicUuid = intent.getStringExtra(EXTRA_CHARACTERISTIC);
                BluetoothGattCharacteristic enableCharacteristic = enableService.getCharacteristic(UUID.fromString(enableCharacteristicUuid));
                if(enableCharacteristic == null) {
                    Log.w(TAG, "Could not Find Characteristic");
                    return RETURN_POLICY;
                }

                bleActionModel = new BleActionModel(action, gatt, enableCharacteristic);
                message.obj = bleActionModel;

                break;
            case DISABLE_NOTIFICATION:
                /**
                 * Valid DISABLE_NOTIFICATION Action Packet
                 *      BleAction {
                 *          BleAction = READ_CHARACTERISTIC
                 *          Bluetooth GATT = <valid BluetoothGatt>
                 *          Characteristic = <valid BluetoothGattCharacteristic>
                 *      }
                 */

                if(!mConnectedDevices.containsKey(bd_addr)) {
                    Log.w(TAG, "Device is  not Connected");
                    return RETURN_POLICY;
                }

                gatt = mConnectedDevices.get(bd_addr);
                if(gatt == null) {
                    Log.w(TAG, "GATT return NULL (Not Connected Properly)");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_SERVICE)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                String disableServiceUuid = intent.getStringExtra(EXTRA_SERVICE);

                BluetoothGattService disableService = gatt.getService(UUID.fromString(disableServiceUuid));
                if(disableService == null) {
                    Log.w(TAG, "Could not Find Service");
                    return RETURN_POLICY;
                }

                if(!intent.hasExtra(EXTRA_CHARACTERISTIC)) {
                    Log.w(TAG, "Invalid READ_CHARACTERISTIC Action Packet");
                    return RETURN_POLICY;
                }
                String disableCharacteristicUuid = intent.getStringExtra(EXTRA_CHARACTERISTIC);
                BluetoothGattCharacteristic disableCharacteristic = disableService.getCharacteristic(UUID.fromString(disableCharacteristicUuid));
                if(disableCharacteristic == null) {
                    Log.w(TAG, "Could not Find Characteristic");
                    return RETURN_POLICY;
                }

                bleActionModel = new BleActionModel(action, gatt, disableCharacteristic);
                message.obj = bleActionModel;

                break;
            case DISCONNECT:
                /**
                 * Valid DISCONNECT Action Packet
                 *      BleAction {
                 *          BleAction = DISCONNECT
                 *          BluetoothGatt = <valid Bluetooth GATT>
                 *      }
                 */

                if(!mConnectedDevices.containsKey(bd_addr)) {
                    Log.w(TAG, "Device is  not Connected");
                    return RETURN_POLICY;
                }

                gatt = mConnectedDevices.get(bd_addr);
                if(gatt == null) {
                    Log.w(TAG, "GATT return NULL (Not Connected Properly)");
                    return RETURN_POLICY;
                }

                bleActionModel = new BleActionModel(action, gatt);
                message.obj = bleActionModel;

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
        Toast.makeText(mContext, "Connecting to " + bluetoothDeviceAddress, Toast.LENGTH_LONG).show();
    }

    private void discoverServices(BluetoothGatt gatt) {
        if(!mConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
            Log.w(TAG, "Requested Device not Connected");
            return;
        }

        gatt.discoverServices();
    }

    private void readCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(!mConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
            Log.w(TAG, "Requested Device not Connected");
            return;
        }

        gatt.readCharacteristic(characteristic);
    }

    private void writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if(!mConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
            Log.w(TAG, "Requested Device not Connected");
            return;
        }

        characteristic.setValue(value);

        gatt.writeCharacteristic(characteristic);
    }

    private void enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(!mConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
            Log.w(TAG, "Requested Device not Connected");
            return;
        }

        gatt.setCharacteristicNotification(characteristic, true);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(NOTIFICATION_DESCRIPTOR));
        if(descriptor == null) {
            Log.w(TAG, "Could not find Notification Descriptor");
            return;
        }

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }

    private void disableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(!mConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
            Log.w(TAG, "Requested Device not Connected");
            return;
        }

        gatt.setCharacteristicNotification(characteristic, false);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(NOTIFICATION_DESCRIPTOR));
        if(descriptor == null) {
            Log.w(TAG, "Could not find Notification Descriptor");
            return;
        }

        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    }

    private void disconnect(BluetoothGatt gatt) {
        if(!mConnectedDevices.containsKey(gatt.getDevice().getAddress())) {
            Log.w(TAG, "Requested Device not Connected");
            return;
        }

        gatt.disconnect();
    }

    private void log(String message) {
        Log.d(TAG, message);
    }

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT Error");
                return;
            }

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    log("Connected to " + gatt.getDevice().getAddress());
                    mConnectedDevices.put(gatt.getDevice().getAddress(), gatt);

                    BleUpdateReceiver.UPDATE_CONNECTED(mContext, gatt.getDevice().getAddress());

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    log("Disconnected from " + gatt.getDevice().getAddress());
                    mConnectedDevices.remove(gatt.getDevice().getAddress());
                    gatt.close(); // NULL Pointer?

                    BleUpdateReceiver.UPDATE_DISCONNECTED(mContext, gatt.getDevice().getAddress());
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT Error");
                return;
            }

            log("Services Discovered on " + gatt.getDevice().getAddress());

            BleUpdateReceiver.UPDATE_SERVICES_DISCOVERED(mContext, gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT Error");
                return;
            }

            log("Characteristic " + characteristic.getUuid().toString() + " Read from Device " + gatt.getDevice().getAddress());

            BleUpdateReceiver.UPDATE_CHARACTERISTIC_READ(mContext, gatt, characteristic, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT Error");
                return;
            }

            log("Characteristic " + characteristic.getUuid().toString() + " Written to Device " + gatt.getDevice().getAddress());

            BleUpdateReceiver.UPDATE_CHARACTERISTIC_WRITTEN(mContext, gatt, characteristic, characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            log("Characteristic " + characteristic.getUuid().toString() + " Updated from Device " + gatt.getDevice().getAddress());

            BleUpdateReceiver.UPDATE_CHARACTERISTIC_NOTIFIED(mContext, gatt, characteristic, characteristic.getValue());
        }
    };
}
