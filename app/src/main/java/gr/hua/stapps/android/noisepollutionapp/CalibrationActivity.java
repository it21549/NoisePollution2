package gr.hua.stapps.android.noisepollutionapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
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

import java.util.Arrays;
import java.util.Objects;

import gr.hua.stapps.android.noisepollutionapp.databinding.ActivityCalibrationBinding;
import gr.hua.stapps.android.noisepollutionapp.network.sockets.CreateConnection;
import gr.hua.stapps.android.noisepollutionapp.network.sockets.DataTransfer;
import gr.hua.stapps.android.noisepollutionapp.viewmodel.CalibrationViewModel;

public class CalibrationActivity extends AppCompatActivity {

    private static final String LOG = "CalibrationActivity ->";
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private String deviceName = "ESP32Test";

    public static CreateConnection createConnection;
    public static DataTransfer dataTransfer;

    private ActivityCalibrationBinding binding;
    private CalibrationViewModel calibrationViewModel;
    public static Handler handler;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println(LOG + "action is: " + action);
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
                    System.out.println(LOG + "deviceName= " + deviceName + " and mac= " + deviceHardwareAddress + " and UUID= " + Arrays.toString(device.getUuids()));

                    if (Objects.equals(deviceName, "ESP32test")) {
                        String deviceAddress = device.getAddress();
                        System.out.println(LOG + "connecting to " + deviceAddress);
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        createConnection = new CreateConnection(
                                bluetoothAdapter,
                                deviceAddress,
                                context,
                                handler,
                                dataTransfer
                        );
                        createConnection.start();
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

        // Get values from previous activity
        Intent intent = getIntent();
        String key = intent.getStringExtra("Key");
        System.out.println(LOG + "value passed is " + key);

        setupReceiver();
        calibrationViewModel = new ViewModelProvider(this).get(CalibrationViewModel.class);
        handler = initHandler(deviceName);
        initListeners();
        initObservers();
    }

    public void initListeners() {
        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calibrationViewModel.initializeContext(CalibrationActivity.this);
            }
        });

        binding.buttonCalibrationGroupI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createConnection.getDataTransfer().write("RECORD0");
            }
        });
        binding.buttonCalibrationGroupII.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createConnection.getDataTransfer().write("RECORD1");
            }
        });
        binding.buttonCalibrationGroupIII.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createConnection.getDataTransfer().write("RECORD2");
            }
        });
        binding.buttonCalibrationGroupIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createConnection.getDataTransfer().write("STOP");
            }
        });
    }

    public void initObservers() {
        final Observer<Boolean> calibration_observer = isBtEnabled -> {
            if (isBtEnabled != null) {
                if (isBtEnabled) {
                    System.out.println(LOG + "bluetooth is enabled");
                    calibrationViewModel.calibrate();
                } else {
                    System.out.println(LOG + "will ask for bluetooth to be enabled");
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
                }
            } else
                System.out.println(LOG + "isBtEnabled is null");
        };
        calibrationViewModel.getIsBtEnabled().observe(this, calibration_observer);
    }

    public Handler initHandler(String deviceName) {
        return handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                System.out.println("NoisePollution " + "connected to " + deviceName);
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
    }

    public void setupReceiver() {
        // Register for broadcasts when a device is discovered.
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

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnection != null) {
            createConnection.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

}