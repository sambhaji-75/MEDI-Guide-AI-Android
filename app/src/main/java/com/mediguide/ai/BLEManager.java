package com.mediguide.ai;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BLEManager {

    public static final UUID SERVICE_UUID =
            UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID RX_UUID =
            UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID TX_UUID =
            UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    private final Context context;
    private final BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic rxCharacteristic;
    private ScanCallback activeScanCallback;

    public BLEManager(Context context) {
        this.context = context.getApplicationContext();
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startScan(ScanCallback callback) {
        if (adapter == null || !adapter.isEnabled()) return;

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return;

        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        activeScanCallback = callback;
        scanner.startScan(callback);
    }

    public void stopScan() {
        if (adapter == null) return;
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return;

        if (activeScanCallback != null) {
            adapter.getBluetoothLeScanner().stopScan(activeScanCallback);
            activeScanCallback = null;
        }
    }

    public void connect(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        gatt = device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    g.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt g, int status) {
                BluetoothGattService service = g.getService(SERVICE_UUID);
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(RX_UUID);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt g,
                                                BluetoothGattCharacteristic characteristic) {
                byte[] data = characteristic.getValue();
                String message = new String(data, StandardCharsets.UTF_8);
                // Example: ESP32 sends "TAKEN"
                if ("TAKEN".equalsIgnoreCase(message)) {
                    // Forward this event to your UI/Firebase log layer.
                }
            }
        });
    }

    public void sendCommand(String command) {
        if (gatt == null || rxCharacteristic == null) return;

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        rxCharacteristic.setValue(command.getBytes(StandardCharsets.UTF_8));
        gatt.writeCharacteristic(rxCharacteristic);
    }

    public void close() {
        if (gatt != null) {
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.disconnect();
                gatt.close();
            }
            gatt = null;
        }
    }
}
