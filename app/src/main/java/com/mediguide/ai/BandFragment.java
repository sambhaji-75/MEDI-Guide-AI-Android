
package com.mediguide.ai;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BandFragment extends Fragment {

    private static final UUID SERVICE_UUID =
            UUID.fromString("12345678-1234-1234-1234-1234567890AB");

    private static final UUID CHARACTERISTIC_UUID =
            UUID.fromString("12345678-1234-1234-1234-1234567890AC");

    private TextView txtBandStatus;
    private TextView txtBandDevice;
    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnVibrate;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic alertCharacteristic;

    private boolean scanning = false;
    private boolean connected = false;
    private boolean writeInProgress = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (!isAdded()) return;

                    if (hasBluetoothPermissions()) {
                        startScanning();
                    } else {
                        setStatus("Bluetooth permissions required");
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.activity_band_fragment,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        txtBandStatus = view.findViewById(R.id.txtBandStatus);
        txtBandDevice = view.findViewById(R.id.txtBandDevice);
        btnConnect = view.findViewById(R.id.btnConnectBand);
        btnDisconnect = view.findViewById(R.id.btnDisconnectBand);
        btnVibrate = view.findViewById(R.id.btnTestVibration);

        BluetoothManager manager = (BluetoothManager)
                requireContext().getSystemService(Context.BLUETOOTH_SERVICE);

        if (manager != null) {
            bluetoothAdapter = manager.getAdapter();
        }

        btnConnect.setOnClickListener(v -> checkPermissionsAndScan());
        btnDisconnect.setOnClickListener(v -> disconnectBand());
        btnVibrate.setOnClickListener(v -> sendVibrationCommand());

        setStatus("Not connected");
        txtBandDevice.setText("ESP32 band not connected");
        updateButtons();
    }

    private boolean hasBluetoothPermissions() {
        Context context = getContext();
        if (context == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED;
        }

        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkPermissionsAndScan() {
        if (bluetoothAdapter == null) {
            setStatus("Bluetooth is not supported on this phone");
            return;
        }

        if (!hasBluetoothPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                });
            } else {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                });
            }
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                setStatus("Turn on Bluetooth, then tap Connect");
                Toast.makeText(
                        requireContext(),
                        "Please enable Bluetooth",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            startScanning();

        } catch (SecurityException e) {
            setStatus("Bluetooth permission denied");
        }
    }

    private void startScanning() {
        if (!isAdded() || bluetoothAdapter == null) return;

        if (!hasBluetoothPermissions()) {
            setStatus("Bluetooth permissions required");
            return;
        }

        try {
            scanner = bluetoothAdapter.getBluetoothLeScanner();

            if (scanner == null) {
                setStatus("BLE scanner unavailable");
                return;
            }

            setStatus("Searching for Medi-Care Band...");
            txtBandDevice.setText("Keep the ESP32 band powered on");

            scanning = true;
            updateButtons();

            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            scanner.startScan(null, settings, scanCallback);

            handler.postDelayed(() -> {
                if (scanning) {
                    stopScanning();

                    if (!connected) {
                        setStatus("Band not found. Check ESP32 power.");
                    }
                }
            }, 12000);

        } catch (SecurityException e) {
            scanning = false;
            setStatus("Bluetooth permission denied");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();

            try {
                String name = device.getName();

                if ((name == null || name.isEmpty())
                        && result.getScanRecord() != null) {
                    name = result.getScanRecord().getDeviceName();
                }

                if (name != null
                        && name.toLowerCase().contains("medi-care")) {
                    stopScanning();
                    connectToDevice(device, name);
                }

            } catch (SecurityException e) {
                setStatus("Bluetooth permission denied");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            scanning = false;
            setStatus("Bluetooth scan failed: " + errorCode);
        }
    };

    private void connectToDevice(BluetoothDevice device, String name) {
        if (!isAdded() || !hasBluetoothPermissions()) {
            setStatus("Bluetooth permission required");
            return;
        }

        try {
            setStatus("Connecting to band...");
            txtBandDevice.setText(name);

            bluetoothGatt = device.connectGatt(
                    requireContext(),
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
            );

        } catch (SecurityException e) {
            setStatus("Bluetooth permission denied");
        }
    }

    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(
                        BluetoothGatt gatt,
                        int status,
                        int newState
                ) {
                    if (newState == BluetoothProfile.STATE_CONNECTED && status == 0) {
                        connected = true;

                        if (!hasBluetoothPermissions()) {
                            setStatus("Bluetooth permission required");
                            return;
                        }

                        try {
                            gatt.discoverServices();
                        } catch (SecurityException e) {
                            setStatus("Bluetooth permission denied");
                        }

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        connected = false;
                        alertCharacteristic = null;
                        writeInProgress = false;

                        try {
                            gatt.close();
                        } catch (Exception ignored) {
                        }

                        if (bluetoothGatt == gatt) {
                            bluetoothGatt = null;
                        }

                        setStatus("Band disconnected");
                    }
                }

                @Override
                public void onServicesDiscovered(
                        BluetoothGatt gatt,
                        int status
                ) {
                    BluetoothGattService service = null;
                    BluetoothGattCharacteristic characteristic = null;

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        service = gatt.getService(SERVICE_UUID);

                        if (service != null) {
                            characteristic =
                                    service.getCharacteristic(CHARACTERISTIC_UUID);
                        }
                    }

                    alertCharacteristic = characteristic;

                    if (characteristic != null) {
                        setStatus("Connected • Medi-Care Band ready");
                    } else {
                        setStatus("Connected, but BLE service not found");
                    }
                }

                @Override
                public void onCharacteristicWrite(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        int status
                ) {
                    writeInProgress = false;

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        setStatus("Vibration command sent");
                    } else {
                        setStatus("Vibration command failed");
                    }
                }
            };

    private void sendVibrationCommand() {
        if (!connected || bluetoothGatt == null
                || alertCharacteristic == null) {
            setStatus("Connect to the band first");
            return;
        }

        if (writeInProgress) return;

        if (!hasBluetoothPermissions()) {
            setStatus("Bluetooth permission required");
            return;
        }

        try {
            byte[] command = "VIBRATE".getBytes(StandardCharsets.UTF_8);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int result = bluetoothGatt.writeCharacteristic(
                        alertCharacteristic,
                        command,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                );

                if (result != BluetoothStatusCodes.SUCCESS) {
                    setStatus("Could not send vibration command");
                    return;
                }

            } else {
                alertCharacteristic.setWriteType(
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                );
                alertCharacteristic.setValue(command);

                if (!bluetoothGatt.writeCharacteristic(alertCharacteristic)) {
                    setStatus("Could not send vibration command");
                    return;
                }
            }

            writeInProgress = true;
            setStatus("Sending vibration command...");

        } catch (SecurityException e) {
            setStatus("Bluetooth permission denied");
        }
    }

    private void stopScanning() {
        if (scanner != null && scanning) {
            try {
                if (hasBluetoothPermissions()) {
                    scanner.stopScan(scanCallback);
                }
            } catch (SecurityException ignored) {
            }
        }

        scanning = false;
        updateButtons();
    }

    private void disconnectBand() {
        stopScanning();

        BluetoothGatt gatt = bluetoothGatt;
        bluetoothGatt = null;

        connected = false;
        alertCharacteristic = null;
        writeInProgress = false;

        if (gatt != null) {
            try {
                if (hasBluetoothPermissions()) {
                    gatt.disconnect();
                }
            } catch (SecurityException ignored) {
            }

            try {
                gatt.close();
            } catch (Exception ignored) {
            }
        }

        setStatus("Disconnected");

        if (txtBandDevice != null) {
            txtBandDevice.setText("ESP32 band not connected");
        }
    }

    private void setStatus(String message) {
        if (!isAdded()) return;

        handler.post(() -> {
            if (!isAdded() || getView() == null) return;

            if (txtBandStatus != null) {
                txtBandStatus.setText(message);
            }

            updateButtons();
        });
    }

    private void updateButtons() {
        if (!isAdded() || getView() == null || btnConnect == null) {
            return;
        }

        btnConnect.setEnabled(!connected && !scanning);

        if (btnDisconnect != null) {
            btnDisconnect.setEnabled(connected || scanning);
        }

        if (btnVibrate != null) {
            btnVibrate.setEnabled(
                    connected && alertCharacteristic != null
            );
        }
    }

    @Override
    public void onDestroyView() {
        stopScanning();

        BluetoothGatt gatt = bluetoothGatt;
        bluetoothGatt = null;

        connected = false;
        alertCharacteristic = null;
        writeInProgress = false;

        if (gatt != null) {
            try {
                if (hasBluetoothPermissions()) {
                    gatt.disconnect();
                }
            } catch (SecurityException ignored) {
            }

            try {
                gatt.close();
            } catch (Exception ignored) {
            }
        }

        txtBandStatus = null;
        txtBandDevice = null;
        btnConnect = null;
        btnDisconnect = null;
        btnVibrate = null;

        super.onDestroyView();
    }
}
