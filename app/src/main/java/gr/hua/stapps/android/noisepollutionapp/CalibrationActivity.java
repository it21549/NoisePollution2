package gr.hua.stapps.android.noisepollutionapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import gr.hua.stapps.android.noisepollutionapp.databinding.ActivityCalibrationBinding;

public class CalibrationActivity extends AppCompatActivity {

    private ActivityCalibrationBinding binding;
    private CalibrationViewModel calibrationViewModel;
    private static final String LOG_INTRO = "CalibrationActivity -> ";

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;

    private ConnectionThread connectionThread;
    public static Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case CONNECTING_STATUS:
                    switch (msg.arg1) {
                        case 1:
                            System.out.println("NoisePollution " + "connected to " + "ESP32");
                            break;
                        case -1:
                            System.out.println("NoisePollution " + "device fails to connect");
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    String arduinoMsg = msg.obj.toString(); //Read message from Arduino
                    System.out.println("NoisePollution " + "the message received is: " + arduinoMsg);
            }
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.getGlobal().log(Level.INFO, LOG_INTRO + "action is: " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(CalibrationActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); //MAC address
                    System.out.println(LOG_INTRO + "deviceName= " + deviceName + " and mac= " + deviceHardwareAddress + " and UUID= " + Arrays.toString(device.getUuids()));

                    if (Objects.equals(deviceName, "ESP32test")) {
                        String deviceAddress = device.getAddress();
                        System.out.println(LOG_INTRO + "connecting to " + deviceAddress);
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        connectionThread = new ConnectionThread(bluetoothAdapter, deviceAddress, handler);
                        connectionThread.start();
                    }
                    return;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalibrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Register for broadcasts when a device is discovered.
        registerReceivers();

        //Provide ViewModel
        calibrationViewModel = new ViewModelProvider(this).get(CalibrationViewModel.class);

        // Get values from previous activity
        Intent intent = getIntent();
        String key = intent.getStringExtra("Key");
        System.out.println(LOG_INTRO + "value passed is " + key);

        setListeners();
        setObservers();
    }

    @SuppressLint("MissingPermission")
    public void setObservers() {
        final Observer<Boolean> calibration_observer = isBluetoothEnabled -> {
            if (isBluetoothEnabled != null) {
                if (isBluetoothEnabled) {
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + "bluetooth is enabled");
                    calibrationViewModel.calibrate();
                } else {
                    Logger.getGlobal().log(Level.INFO, LOG_INTRO + "asking to enable bluetooth");
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
                }
            } else
                Logger.getGlobal().log(Level.INFO, LOG_INTRO + "isBluetoothEnabled is false");
        };
        calibrationViewModel.getIsBluetoothEnabled().observe(this, calibration_observer);
    }

    public void setListeners() {
        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Use this check to determine whether Bluetooth classic is supported on the device.
                // Then you can selectively disable BLE-related features.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    Toast.makeText(CalibrationActivity.this, "R.string.bluetooth_not_supported", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    if (ActivityCompat.checkSelfPermission(CalibrationActivity.this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                        calibrationViewModel.initializeContext(CalibrationActivity.this);
                    } else
                        Toast.makeText(CalibrationActivity.this, "permission for bluetooth not granted", Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.buttonCalibrationGroupI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionThread.getDataThread().write("RECORD0");
            }
        });
        binding.buttonCalibrationGroupII.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionThread.getDataThread().write("RECORD1");
            }
        });
        binding.buttonCalibrationGroupIII.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionThread.getDataThread().write("RECORD2");
            }
        });
        binding.buttonCalibrationGroupIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectionThread.getDataThread().write("STOP");
            }
        });
    }

    public void registerReceivers() {
        IntentFilter actionFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter actionDiscoveryStartedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, actionFoundFilter);
        registerReceiver(receiver, actionDiscoveryStartedFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }
}