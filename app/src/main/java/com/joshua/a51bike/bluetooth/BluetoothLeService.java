/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.joshua.a51bike.bluetooth;

import android.annotation.SuppressLint;
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
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 *BLE链接蓝牙的Service,负责和BLE设备进行通信
 */
@SuppressLint("NewApi")
public class BluetoothLeService extends Service {

    private final static String TAG = "BikeControl";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    //设备主动上报信息的地址
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    //Gatt协议通信回调
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        //连接回调
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange: STATE_CONNECTED ");
                intentAction = ACTION_GATT_CONNECTED;
                //保存当前状态
                mConnectionState = STATE_CONNECTED;
                //广播通知外层
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                //连接成功，开始发现服务
                mBluetoothGatt.discoverServices();
                Log.i(TAG, "Attempting to start service discovery");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                //连接失败，广播通知外层
                broadcastUpdate(intentAction);
            }
        }

        //发现服务后的回调
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered: ");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //发现服务，广播通知外层
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                for (int i = 0; i < characteristic.getValue().length; i++) {
                    System.out.println(i + "--------read success----- characteristic:" + characteristic.getValue()[i]);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            System.out.println("onDescriptorWrite = " + status
                    + ", descriptor =" + descriptor.getUuid().toString());
        }

        //信号强度
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }

        //向BLE设备写入成功后的回调
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            for (int i = 0; i < characteristic.getValue().length; i++) {
                int result = characteristic.getValue()[i] & 0xff;
                System.out.println(i + "--------write success----- characteristic:" + result);
            }
        }

        //获取BLE设备返回信息的回调
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] resultBytes=new byte[8];
            for (int i = 0; i < (characteristic.getValue().length-1); i++) {
                resultBytes[i]=characteristic.getValue()[i];
                System.out.println(i + "--------change success----- characteristic:" + resultBytes[i]);
            }
            broadcastUpdate(ACTION_DATA_AVAILABLE, resultBytes);
        }
    };

    //广播通知外层，状态
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    //广播通知外层，状态，设备回复信息
    private void broadcastUpdate(final String action,final byte[] bytes) {
        final Intent intent = new Intent(action);
        intent.putExtra("resultBytes",bytes);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        Log.i(TAG, "broadcastUpdate: ");
        final Intent intent = new Intent(action);
        System.out.println("change broadcast update:uuid=" + characteristic.getUuid().toString());
        final byte[] data = characteristic.getValue();
        System.out.println(Arrays.toString(data));
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(
                    data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));

            System.out.println("ppp" + new String(data) + "\n"
                    + stringBuilder.toString());
            intent.putExtra(EXTRA_DATA, new String(data) + "\n"
                    + stringBuilder.toString());
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * 返回初始化
     * mBluetoothManager与mBluetoothAdapter的结果
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * 通过gatt协议连接ble设备
     * 连接失败就返回false
     */
    public boolean connect(final String address) {
        //判断mBluetoothAdapter与address是否存在
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        //先前连接的设备。 尝试重新连接
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        //获取ble设备
        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        Log.d(TAG, "GET BLe device"+device.getAddress());
        //真正进行连接的方法
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        //保存当前设备的地址，便于下次连接
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        close();
    }

    /**
     * 关闭连接
     * 必须调用以防内存泄漏
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 向ble设备写数据
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);

    }

    /**
     * 从ble设备读数据
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * 设置可观察
     */
    public void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID
                .fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null) {
            Log.w(TAG, "desc not null , set notify");
            descriptor
                    .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * 获取ble设备的service列表
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null){
            Log.d(TAG, "getSupportedGattServices: mBluetoothGatt==null");
            return null;
        }
        Log.d(TAG, "getSupportedGattServices: services size:"+mBluetoothGatt.getServices().size());
        return mBluetoothGatt.getServices();
    }


    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }
}